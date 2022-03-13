package xzc.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import xzc.server.constant.RedisKey;

import java.util.Map;

/**
 * 保存用户加入的房间和游戏，防止断开重连时丢失状态
 */
@Slf4j
@Component
public class UserRelationCache {

    public static final String GAME_ID = "gameId";

    public static final String ROOM_ID = "roomId";

    @Autowired
    private StringRedisTemplate redisTemplate;

    public String getUserRelationKey(Long uid) {
        return RedisKey.makeKey(RedisKey.USER_RELATION, uid);
    }

    public void onJoinRoom(Long uid, Long roomId) {
        redisTemplate.opsForHash().put(getUserRelationKey(uid), ROOM_ID, roomId);
    }

    public void onChangeRoom(Long uid, Long roomId) {
        redisTemplate.opsForHash().put(getUserRelationKey(uid), ROOM_ID, roomId);
    }

    public void onQuitRoom(Long uid, Long roomId) {
        redisTemplate.opsForHash().delete(getUserRelationKey(uid), ROOM_ID);
    }


    public void onStartGame(Long uid, Long gameId) {
        redisTemplate.opsForHash().put(getUserRelationKey(uid), GAME_ID, gameId);
    }

    public void onGameOver(Long uid, Long gameId) {
        redisTemplate.opsForHash().delete(getUserRelationKey(uid), GAME_ID);
    }

    public void onDisconnect(Long uid) {

    }

    public Map<Object, Object> getUserRelation(Long uid) {
        return redisTemplate.opsForHash().entries(getUserRelationKey(uid));
    }
}
