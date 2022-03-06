package xzc.server.service;

import com.google.protobuf.Any;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xzc.server.bean.AliveGame;
import xzc.server.bean.AliveRoom;
import xzc.server.bean.UserInfo;
import xzc.server.proto.*;
import xzc.server.util.BeanConverter;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RoomService {

    @Autowired
    private AliveRoomHolder aliveRoomHolder;

    @Autowired
    private PushService pushService;

    public void quickJoin(UserInfo userInfo, QuickJoinRoomRequest quickJoinRoomRequest) throws Exception {
        // 选择或者创建一个房间
        AliveRoom aliveRoom = aliveRoomHolder.getOrCreateRoom(quickJoinRoomRequest.getRoomType());
        // 加入房间
        aliveRoomHolder.join(aliveRoom, userInfo);
        // 通知客户端
        QuickJoinRoomResponse quickJoinRoomResponse = QuickJoinRoomResponse.newBuilder()
                .setRoomId(aliveRoom.getRoomId())
                .putAllParticipants(aliveRoom.getMembersMap().values()
                        .stream()
                        .map(BeanConverter::member2Participant)
                        .collect(Collectors.toMap(Participant::getUid, Function.identity())))
                .build();
        XZCSignal xzcSignal = XZCSignal.newBuilder()
                .setCommand(XZCCommand.QUICK_JOIN_ROOM_RESPONSE)
                .setBody(Any.pack(quickJoinRoomResponse))
                .build();
        // 通知房间内的其他成员
        pushService.batchPushSignal(new ArrayList<>(aliveRoom.getMembersMap().keySet()), xzcSignal);
    }

    public void ready(UserInfo userInfo, ReadyRequest readyRequest) throws Exception {
        // TODO: 2022/3/6 修改状态为已准备
        // 加入房间
        long roomId = readyRequest.getRoomId();
        AliveRoom aliveRoom = aliveRoomHolder.ready(roomId, userInfo);
        // 通知客户端
        ReadyResponse readyResponse = ReadyResponse.newBuilder()
                .setRoomId(roomId)
                .putAllParticipants(aliveRoom.getMembersMap().values()
                        .stream()
                        .map(BeanConverter::member2Participant)
                        .collect(Collectors.toMap(Participant::getUid, Function.identity())))
                .build();
        XZCSignal xzcSignal = XZCSignal.newBuilder()
                .setCommand(XZCCommand.READY_RESPONSE)
                .setBody(Any.pack(readyResponse))
                .build();
        // 通知房间内的其他成员
        pushService.batchPushSignal(new ArrayList<>(aliveRoom.getMembersMap().keySet()), xzcSignal);
    }

    public AliveGame.Gamer userInfoToGamer(UserInfo userInfo) {
        return new AliveGame.Gamer()
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
