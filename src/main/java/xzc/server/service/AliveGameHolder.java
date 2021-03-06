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
                    throw new XzcException(ErrorCode.ERROR_CODE_INTERNAL_SERVER_ERROR, "?????????????????????????????????????????????: " + operator + ", gameId: " + gameId);
                }
                List<Card> cardLibrary = aliveGame.getCardLibrary();
                // ?????????????????????
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
                    throw new XzcException(ErrorCode.ERROR_CODE_INTERNAL_SERVER_ERROR, "?????????????????????????????????????????????: " + operator + ", gameId: " + gameId);
                }
                // ??????????????????
                if (card == gamer.getDrewCard()) {
                    gamer.setHandCard(card)
                            .setDrewCard(null);
                } else if (card == gamer.getHandCard()) {
                    gamer.setDrewCard(null);
                } else {
                    throw new XzcException(ErrorCode.ERROR_CODE_INTERNAL_SERVER_ERROR, "Not Happened, ????????????????????????: " + operator + ", gameId: " + gameId + ", card: " + card.name());
                }

                // ????????????
                gamer.setState(AliveGame.GamerState.WAIT)
                        .setEvent(AliveGame.GamerEvent.DISCARD);
                // ?????????????????????????????????
                int orderedGamersIndex = aliveGame.getOrderedGamersIndex();
                if (orderedGamersIndex == aliveGame.getOrderlyGamers().size() - 1) {
                    // ??????????????? part???????????????
                    orderedGamersIndex = 0;
                    Long gamerId = aliveGame.getOrderlyGamers().get(orderedGamersIndex);
                    aliveGame.getGamerMap().get(gamerId)
                            .setState(AliveGame.GamerState.BET);
                } else {
                    orderedGamersIndex++;
                    Long gamerId = aliveGame.getOrderlyGamers().get(orderedGamersIndex);
                    // ??????????????????
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
                    throw new XzcException(ErrorCode.ERROR_CODE_INTERNAL_SERVER_ERROR, "?????????????????????????????????????????????: " + operator + ", gameId: " + gameId);
                }
                List<Card> cardLibrary = aliveGame.getCardLibrary();
                // ?????????????????????
                int cardLibraryIndex = aliveGame.getCardLibraryIndex();
                Card xzcCard = cardLibrary.get(cardLibraryIndex);
                // ??????????????????????????????
                aliveGame.getDiscardPile().add(aliveGame.getXzcCard());
                aliveGame.setXzcCard(xzcCard)
                        .setCardLibraryIndex(cardLibraryIndex + 1);
                // ????????????
                gamer.setState(AliveGame.GamerState.WAIT)
                        .setEvent(AliveGame.GamerEvent.CHANGE_XZC_CARD);
                // ?????????????????????????????????
                int orderedGamersIndex = aliveGame.getOrderedGamersIndex();
                if (orderedGamersIndex == aliveGame.getOrderlyGamers().size() - 1) {
                    // ???????????????unit???????????????
                    orderedGamersIndex = 0;
                    Long gamerId = aliveGame.getOrderlyGamers().get(orderedGamersIndex);
                    aliveGame.getGamerMap().get(gamerId)
                            .setState(AliveGame.GamerState.BET);
                } else {
                    orderedGamersIndex++;
                    Long gamerId = aliveGame.getOrderlyGamers().get(orderedGamersIndex);
                    // ??????????????????
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
            // todo ???????????????
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

        // ???????????????????????????????????????true?????????????????????aliveGame
        protected boolean changed = true;

        // ????????????????????????????????????????????????????????????????????????setIgnoreException??????true
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
                //??????????????????????????????
                changed = false;
                if (ignoreException) {
                    log.warn("Handle game: {} operate is exception???ignore this???{}???", gameId, e);
                    return null;
                } else {
                    throw e;
                }
            } finally {
                if (changed) {
                    //?????????????????? gamerMap
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
