package xzc.server.service;

import com.google.protobuf.Any;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import xzc.server.bean.AliveRoom;
import xzc.server.bean.Gamer;
import xzc.server.bean.UserInfo;
import xzc.server.constant.RedisKey;
import xzc.server.proto.*;

import java.util.ArrayList;

@Service
@Slf4j
public class RoomService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AliveRoomHolder aliveRoomHolder;

    @Autowired
    private PushService pushService;

    public void quickJoin(UserInfo userInfo, QuickJoinRoomRequest quickJoinRoomRequest) {
        // 选择或者创建一个房间
        AliveRoom aliveRoom = aliveRoomHolder.getOrCreateRoom(quickJoinRoomRequest.getRoomType());
        // TODO: 2022/2/15 加入房间
        long joinTime = System.currentTimeMillis();
        Participant participant = userInfoToParticipant(userInfo)
                .toBuilder()
                .setEvent(ParticipantEvent.JOIN)
                .setState(ParticipantState.IDLE).build();
//                .setJoinTime(joinTime)
//                .setLastStateChange(joinTime);
        aliveRoom.getParticipantMap().put(participant.getUid(), participant);
        QuickJoinRoomResponse quickJoinRoomResponse = QuickJoinRoomResponse.newBuilder()
                .setRoomId(aliveRoom.getRoomId())
                .putAllParticipants(aliveRoom.getParticipantMap())
                .build();
        XZCSignal xzcSignal = XZCSignal.newBuilder()
                .setCommand(XZCCommand.QUICK_JOIN_ROOM_RESPONSE)
                .setBody(Any.pack(quickJoinRoomResponse))
                .build();
        // 通知房间内的其他成员
        pushService.batchPushSignal(new ArrayList<>(aliveRoom.getParticipantMap().keySet()), xzcSignal);
    }


    public Gamer userInfoToGamer(UserInfo userInfo) {

        return new Gamer()
                .setUid(userInfo.getUid())
                .setNickname(userInfo.getNickname())
                .setAvatar(userInfo.getAvatar());
    }


    public Participant userInfoToParticipant(UserInfo userInfo) {
        return Participant.newBuilder()
                .setUid(userInfo.getUid())
                .setNickname(userInfo.getNickname())
                .setAvatar(userInfo.getAvatar())
                .build();
    }


}
