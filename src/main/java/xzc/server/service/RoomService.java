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

    public static final int MIN_START_NUM = 3;

    public static final int MAX_START_NUM = 6;

    @Autowired
    private AliveRoomHolder aliveRoomHolder;

    @Autowired
    private GameService gameService;

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
        pushService.pushSignal(userInfo.getUid(), xzcSignal);
        // 通知房间内的其他成员
        pushService.batchPushSignal(new ArrayList<>(aliveRoom.getMembersMap().keySet()), xzcSignal);

    }

    public void ready(UserInfo userInfo, ReadyRequest readyRequest) throws Exception {
        // 加入房间
        long roomId = readyRequest.getRoomId();
        AliveRoom aliveRoom = aliveRoomHolder.ready(roomId, userInfo, readyRequest.getReadyOrCancel());
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

    /**
     * 开始游戏
     *
     * @param userInfo
     * @param startRequest
     * @throws Exception
     */
    public void start(UserInfo userInfo, StartRequest startRequest) throws Exception {
        long roomId = startRequest.getRoomId();
        // 修改房间状态
        AliveRoom aliveRoom = aliveRoomHolder.start(roomId, userInfo.getUid());
        // TODO: 2022/3/9 创建游戏并加入
        AliveGame aliveGame = gameService.create(aliveRoom.getMembersMap());
        // TODO: 2022/3/9 回复玩家，
        // todo 通知其他所有玩家
    }

    /**
     * 退出房间
     *
     * @param userInfo
     * @param quitRequest
     * @throws Exception
     */
    public void quit(UserInfo userInfo, QuitRequest quitRequest) throws Exception {
        long roomId = quitRequest.getRoomId();
        // 修改房间状态
        AliveRoom aliveRoom = aliveRoomHolder.quit(roomId, userInfo.getUid());
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
