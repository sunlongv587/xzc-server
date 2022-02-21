package xzc.server.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import xzc.server.bean.AliveGame;
import xzc.server.constant.RedisKey;

import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GameService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private String getRoomKey(Long gameId) {
        return RedisKey.makeKey(RedisKey.GAME, gameId);
    }

    private String getRoomLockKey(Long gameId) {
        return RedisKey.makeKey(RedisKey.GAME_LOCK, gameId);
    }

    public AliveGame getGame(Long gameId) {
        return (AliveGame) redisTemplate.opsForValue().get(getRoomKey(gameId));
    }

    public void saveGame(AliveGame aliveGame) {
        String roomKey = getRoomKey(aliveGame.getId());
        redisTemplate.opsForValue().set(roomKey, aliveGame, 3, TimeUnit.DAYS);
    }



    public <T> T handleWithSessionLock(long sessionId, Callable<T> handleImpl) throws Exception{
        RLock sLock = redissonClient.getLock(getRoomLockKey(sessionId));
        long timeout = 5000L;
        if (!sLock.tryLock(timeout, timeout,TimeUnit.MILLISECONDS)) {
            //that should not happen.
            log.warn("Try get session lock timeout. {}",sessionId);
            throw new RuntimeException("Get session lock timeout. Please try again");
        }
        try {
            T res = handleImpl.call();
            return res;
        }finally {
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
                //一旦发生异常，不保存session
                changed = false;
                if (ignoreException) {
                    log.warn("Handle game: {} operate is exception，ignore this：{}！", gameId, e);
                }
                return null;
            } finally {
                if (changed) {
                    //保存修改后的aliveSession
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
