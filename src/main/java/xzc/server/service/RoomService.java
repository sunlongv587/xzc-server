package xzc.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xzc.server.bean.AliveRoom;
import xzc.server.bean.Gamer;
import xzc.server.bean.UserInfo;
import xzc.server.proto.ParticipantEvent;
import xzc.server.proto.ParticipantState;
import xzc.server.proto.QuickJoinRoomRequest;

@Service
@Slf4j
public class RoomService {

    @Autowired
    private AliveRoomHolder aliveRoomHolder;

    public void quickJoin(UserInfo userInfo, QuickJoinRoomRequest quickJoinRoomRequest) {
        // 选择或者创建一个房间
        AliveRoom aliveRoom = aliveRoomHolder.getOrCreateRoom(quickJoinRoomRequest.getRoomType());
        // TODO: 2022/2/15 加入房间
        long joinTime = System.currentTimeMillis();
        Gamer gamer = userInfoToGamer(userInfo)
                .setEvent(ParticipantEvent.JOIN)
                .setState(ParticipantState.IDLE)
                .setJoinTime(joinTime)
                .setLastStateChange(joinTime);
        aliveRoom.getGamerMap().put(gamer.getUid(), gamer);
//        aliveRoom
//        quickJoinRoomRequest.get
        // TODO: 2022/2/15 通知房间内的其他成员
    }


    public Gamer userInfoToGamer(UserInfo userInfo) {
        return new Gamer()
                .setUid(userInfo.getUid())
                .setNickname(userInfo.getNickname())
                .setAvatar(userInfo.getAvatar());
    }

}
