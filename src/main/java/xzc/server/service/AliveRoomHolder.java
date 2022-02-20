package xzc.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import xzc.server.bean.AliveRoom;
import xzc.server.constant.RedisKey;
import xzc.server.constant.RoomState;
import xzc.server.proto.RoomType;

import java.util.LinkedHashMap;

@Service
public class AliveRoomHolder {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private IdService idService;

    public String getAliveRoomKey(Long roomId) {
        return RedisKey.makeKey(RedisKey.ALIVE_ROOM, roomId);
    }

    public String getOptionalRoomsKey(RoomType roomType) {
        return RedisKey.makeKey(RedisKey.ALIVE_ROOM, roomType.name());
    }

    public AliveRoom getAliveRoom(Long roomId) {
        return (AliveRoom) redisTemplate.opsForValue().get(getAliveRoomKey(roomId));
    }

    // TODO: 2022/2/16 维护可使用的房间
    public void saveAliveRoom(AliveRoom aliveRoom) {
        redisTemplate.opsForValue().set(getAliveRoomKey(aliveRoom.getRoomId()), aliveRoom);
    }

    public AliveRoom getOrCreateRoom(RoomType roomType) {
        // todo 获取或者创建一个可用的房间
        return createRoom(roomType);
    }

    public AliveRoom createRoom(RoomType roomType) {
        // todo 创建一个房间
        AliveRoom aliveRoom = new AliveRoom()
                .setRoomId(1L)
                .setState(RoomState.OPENED)
                .setParticipantMap(new LinkedHashMap<>());
        // 保存房间
        saveAliveRoom(aliveRoom);
        // TODO: 2022/2/20 保存到一个可选房间集合中，比如建一个zset，使用剩余座位数为score
        return aliveRoom;
    }



}
