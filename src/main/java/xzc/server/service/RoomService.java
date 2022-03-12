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

    @Autowired
    private OptionalRoomCache optionalRoomCache;

    public void quickJoin(UserInfo userInfo, QuickJoinRoomRequest quickJoinRoomRequest) throws Exception {
        // 选择并加入一个房间
        RoomType roomType = quickJoinRoomRequest.getRoomType();
        AliveRoom aliveRoom = aliveRoomHolder.getAndJoinRoomRetry(roomType, userInfo, 5);
        if (aliveRoom == null) {
            // 加入房间
            aliveRoom = aliveRoomHolder.createAndJoinRoom(roomType, userInfo);
        }
        if (aliveRoom == null) {
            // todo 加入失败
            throw new RuntimeException("加入失败");
        }
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
        // 从可选房间列表中将该房间移除（可能不存在）
        optionalRoomCache.remove(aliveRoom.getRoomType(), roomId);
        // 创建游戏并加入
        AliveGame aliveGame = gameService.create(aliveRoom.getMembersMap());
        // 回复玩家，
        StartResponse startResponse = StartResponse.newBuilder()
                .setRoomId(aliveRoom.getRoomId())
                .setGameId(aliveGame.getId())
                .build();
        XZCSignal xzcSignal = XZCSignal.newBuilder()
                .setCommand(XZCCommand.READY_RESPONSE)
                .setBody(Any.pack(startResponse))
                .build();
        pushService.pushSignal(userInfo.getUid(), xzcSignal);
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
        // 从可选房间列表中为该房间新增座位或添加（可能不存在）
        if (aliveRoom.getMembersMap().isEmpty()) {
            // 房间内没有人了，释放这个room
            optionalRoomCache.remove(aliveRoom.getRoomType(), roomId);
        } else {
            optionalRoomCache.incr(aliveRoom.getRoomType(), roomId);
        }
        QuitResponse quitResponse = QuitResponse.newBuilder()
                .setRoomId(aliveRoom.getRoomId())
                .build();
        XZCSignal xzcSignal = XZCSignal.newBuilder()
                .setCommand(XZCCommand.QUIT_RESPONSE)
                .setBody(Any.pack(quitResponse))
                .build();
        pushService.pushSignal(userInfo.getUid(), xzcSignal);
        // todo 通知其他所有玩家
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
