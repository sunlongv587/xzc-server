package xzc.server.service;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import xzc.server.constant.RedisKey;
import xzc.server.proto.room.RoomType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OptionalRoomCache {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public String getOptionalRoomKey(RoomType roomType) {
        return RedisKey.makeKey(RedisKey.OPTIONAL_ROOMS, roomType.name());
    }

    public void add(RoomType roomType, Long roomId, Integer remainSeatCount) {
        String optionalRoomKey = getOptionalRoomKey(roomType);
        redisTemplate.opsForZSet().add(optionalRoomKey, roomId, remainSeatCount);
    }

    public void incr(RoomType roomType, Long roomId) {
        redisTemplate.opsForZSet()
                .incrementScore(getOptionalRoomKey(roomType), roomId, 1);
    }

    public void decr(RoomType roomType, Long roomId) {
        redisTemplate.opsForZSet()
                .incrementScore(getOptionalRoomKey(roomType), roomId, -1);
    }

    public void remove(RoomType roomType, Long roomId) {
        redisTemplate.opsForZSet()
                .remove(getOptionalRoomKey(roomType), roomId);
    }

    public List<Pair<Long, Integer>> getList(RoomType roomType) {
        Set<ZSetOperations.TypedTuple<Object>> typedTuples = redisTemplate.opsForZSet()
                .rangeWithScores(getOptionalRoomKey(roomType), 0, -1);
        if (typedTuples == null) {
            return null;
        }
        return typedTuples.stream()
                .map(typedTuple ->
                        Pair.of((Long) typedTuple.getValue(),
                                typedTuple.getScore() == null ? 0 : typedTuple.getScore().intValue()))
                .collect(Collectors.toList());
    }

}
