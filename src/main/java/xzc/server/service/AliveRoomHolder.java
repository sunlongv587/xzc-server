package xzc.server.service;

import org.springframework.stereotype.Service;
import xzc.server.bean.AliveRoom;
import xzc.server.constant.RoomState;
import xzc.server.proto.RoomType;

import java.util.LinkedHashMap;

@Service
public class AliveRoomHolder {
    // TODO: 2022/2/16 维护可使用的房间

    public AliveRoom getOrCreateRoom(RoomType roomType) {
        // todo 获取或者创建一个可用的房间
        return createRoom(roomType);
    }

    public AliveRoom createRoom(RoomType roomType) {
        // todo 创建一个房间
        return new AliveRoom()
                .setRoomId(1L)
                .setState(RoomState.OPENED)
                .setGamerMap(new LinkedHashMap<>());
    }



}
