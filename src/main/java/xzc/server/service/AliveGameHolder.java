package xzc.server.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import xzc.server.bean.AliveGame;
import xzc.server.constant.Card;
import xzc.server.constant.RedisKey;
import xzc.server.exception.XzcException;
import xzc.server.proto.common.ErrorCode;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AliveGameHolder {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private String getGameKey(Long gameId) {
        return RedisKey.makeKey(RedisKey.GAME, gameId);
    }

    private String getGameLockKey(Long gameId) {
        return RedisKey.makeKey(RedisKey.GAME_LOCK, gameId);
    }

    public AliveGame getGame(Long gameId) {
        return (AliveGame) redisTemplate.opsForValue().get(getGameKey(gameId));
    }

    public void saveGame(AliveGame aliveGame) {
        String gameKey = getGameKey(aliveGame.getId());
        redisTemplate.opsForValue().set(gameKey, aliveGame, 3, TimeUnit.DAYS);
    }

    public AliveGame getAliveGame(Long gameId) {
        return (AliveGame) redisTemplate.opsForValue().get(getGameKey(gameId));
    }

    public AliveGame takeCard(Long gameId, Long userId) throws Exception {
        return handleWithGameLock(gameId, new WithGameCallable<AliveGame>(gameId, userId) {

            @Override
            protected AliveGame innerCall() throws Exception {
                Map<Long, AliveGame.Gamer> gamerMap = aliveGame.getGamerMap();
                AliveGame.Gamer gamer = gamerMap.get(operator);
                if (gamer == null || gamer.getState() != AliveGame.GamerState.ACTION) {
                    throw new XzcException(ErrorCode.ERROR_CODE_INTERNAL_SERVER_ERROR, "玩家不在游戏中或还没轮到该玩家: " + operator + ", gameId: " + gameId);
                }
                List<Card> cardLibrary = aliveGame.getCardLibrary();
                // 从牌库抓一张牌
                int cardLibraryIndex = aliveGame.getCardLibraryIndex();
                gamer.setDrewCard(cardLibrary.get(cardLibraryIndex))
                        .setState(AliveGame.GamerState.DISCARD)
                        .setEvent(AliveGame.GamerEvent.TAKE_CARD);
                aliveGame.setCardLibraryIndex(cardLibraryIndex + 1);
                return aliveGame;
            }

        });
    }


    public AliveGame discard(Long gameId, Long userId, Card card) throws Exception {
        return handleWithGameLock(gameId, new WithGameCallable<AliveGame>(gameId, userId) {

            @Override
            protected AliveGame innerCall() throws Exception {
                Map<Long, AliveGame.Gamer> gamerMap = aliveGame.getGamerMap();
                AliveGame.Gamer gamer = gamerMap.get(operator);
                if (gamer == null || gamer.getState() != AliveGame.GamerState.DISCARD) {
                    throw new XzcException(ErrorCode.ERROR_CODE_INTERNAL_SERVER_ERROR, "玩家不在游戏中或还没轮到该玩家: " + operator + ", gameId: " + gameId);
                }
                // 修改玩家手牌
                if (card == gamer.getDrewCard()) {
                    gamer.setHandCard(card)
                            .setDrewCard(null);
                } else if (card == gamer.getHandCard()) {
                    gamer.setDrewCard(null);
                } else {
                    throw new XzcException(ErrorCode.ERROR_CODE_INTERNAL_SERVER_ERROR, "Not Happened, 玩家的弃牌有问题: " + operator + ", gameId: " + gameId + ", card: " + card.name());
                }

                // 回合结束
                gamer.setState(AliveGame.GamerState.WAIT)
                        .setEvent(AliveGame.GamerEvent.DISCARD);
                // 判断是否是最后一个玩家
                int orderedGamersIndex = aliveGame.getOrderedGamersIndex();
                if (orderedGamersIndex == aliveGame.getOrderlyGamers().size() - 1) {
                    // 开启下一个 part，轮流下注
                    orderedGamersIndex = 0;
                    Long gamerId = aliveGame.getOrderlyGamers().get(orderedGamersIndex);
                    aliveGame.getGamerMap().get(gamerId)
                            .setState(AliveGame.GamerState.BET);
                } else {
                    orderedGamersIndex++;
                    Long gamerId = aliveGame.getOrderlyGamers().get(orderedGamersIndex);
                    // 轮到下个玩家
                    aliveGame.getGamerMap().get(gamerId)
                            .setState(AliveGame.GamerState.ACTION);
                }
                return aliveGame;
            }
        });
    }

    public AliveGame changeXzcCard(Long gameId, Long userId) throws Exception {
        return handleWithGameLock(gameId, new WithGameCallable<AliveGame>(gameId, userId) {

            @Override
            protected AliveGame innerCall() throws Exception {
                Map<Long, AliveGame.Gamer> gamerMap = aliveGame.getGamerMap();
                AliveGame.Gamer gamer = gamerMap.get(operator);
                if (gamer == null || gamer.getState() != AliveGame.GamerState.ACTION) {
                    throw new XzcException(ErrorCode.ERROR_CODE_INTERNAL_SERVER_ERROR, "玩家不在游戏中或还没轮到该玩家: " + operator + ", gameId: " + gameId);
                }
                List<Card> cardLibrary = aliveGame.getCardLibrary();
                // 从牌库拿一张牌
                int cardLibraryIndex = aliveGame.getCardLibraryIndex();
                Card xzcCard = cardLibrary.get(cardLibraryIndex);
                // 旧小早川牌放入弃牌堆
                aliveGame.getDiscardPile().add(aliveGame.getXzcCard());
                aliveGame.setXzcCard(xzcCard)
                        .setCardLibraryIndex(cardLibraryIndex + 1);
                // 回合结束
                gamer.setState(AliveGame.GamerState.WAIT)
                        .setEvent(AliveGame.GamerEvent.CHANGE_XZC_CARD);
                // 判断是否是最后一个玩家
                int orderedGamersIndex = aliveGame.getOrderedGamersIndex();
                if (orderedGamersIndex == aliveGame.getOrderlyGamers().size() - 1) {
                    // 开启下一个unit，轮流下注
                    orderedGamersIndex = 0;
                    Long gamerId = aliveGame.getOrderlyGamers().get(orderedGamersIndex);
                    aliveGame.getGamerMap().get(gamerId)
                            .setState(AliveGame.GamerState.BET);
                } else {
                    orderedGamersIndex++;
                    Long gamerId = aliveGame.getOrderlyGamers().get(orderedGamersIndex);
                    // 轮到下个玩家
                    aliveGame.getGamerMap().get(gamerId)
                            .setState(AliveGame.GamerState.ACTION);
                }
                return aliveGame;
            }
        });
    }

    public <T> T handleWithGameLock(long gameId, Callable<T> handleImpl) throws Exception {
        RLock sLock = redissonClient.getLock(getGameLockKey(gameId));
        long timeout = 5000L;
        if (!sLock.tryLock(timeout, timeout, TimeUnit.MILLISECONDS)) {
            //that should not happen.
            log.warn("Try get alive game lock timeout. {}", gameId);
            throw new RuntimeException("Get alive game lock timeout. Please try again");
        }
        try {
            // todo 使用线程池
            return handleImpl.call();
        } finally {
            sLock.unlockAsync();
        }
    }

    abstract class WithGameCallable<T> implements Callable<T> {

        public WithGameCallable(Long gameId, Long operator) {
            this.operator = operator;
            this.gameId = gameId;
        }

        protected Long gameId;
        protected AliveGame aliveGame;
        protected Long operator;

        // 判断本次操作是否有修改，当true时，自动保存此aliveGame
        protected boolean changed = true;

        // 默认抛出异常，如果不想抛异常，做忽略处理，则调用setIgnoreException置为true
        protected boolean ignoreException = false;

        public void setIgnoreException(boolean ignoreException) {
            this.ignoreException = ignoreException;
        }

        @Override
        public final T call() throws Exception {
            try {
                beforeCall();
                return innerCall();
            } catch (Exception e) {
                //一旦发生异常，不保存
                changed = false;
                if (ignoreException) {
                    log.warn("Handle game: {} operate is exception，ignore this：{}！", gameId, e);
                    return null;
                } else {
                    throw e;
                }
            } finally {
                if (changed) {
                    //保存修改后的 gamerMap
                    aliveGame.setLastChangeTime(System.currentTimeMillis());
                    saveGame(aliveGame);
                }
            }
        }

        protected void beforeCall() throws Exception {
            this.aliveGame = getGame(gameId);

            if (aliveGame == null) {
                log.error("Can not find game: {}", gameId);
                throw new NoSuchElementException("cannot find session " + gameId);
            }
        }

        protected abstract T innerCall() throws Exception;
    }
}
