package xzc.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xzc.server.bean.AliveGame;
import xzc.server.bean.AliveRoom;
import xzc.server.bean.UserInfo;
import xzc.server.exception.XzcException;
import xzc.server.proto.common.ErrorCode;
import xzc.server.proto.common.SignalType;
import xzc.server.proto.room.*;
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

    @Autowired
    private UserRelationCache userRelationCache;

    public void quickJoin(UserInfo userInfo, QuickJoinRoomRequest quickJoinRoomRequest) throws Exception {
        // 选择并加入一个房间
        RoomType roomType = quickJoinRoomRequest.getRoomType();
        AliveRoom aliveRoom = aliveRoomHolder.getAndJoinRoomRetry(roomType, userInfo, 5);
        if (aliveRoom == null) {
            // 加入房间
            aliveRoom = aliveRoomHolder.createAndJoinRoom(roomType, userInfo);
        }
        if (aliveRoom == null) {
            throw new XzcException(ErrorCode.ERROR_CODE_QUICK_JOIN_FAILED, "加入失败");
        }
        // 通知客户端
        QuickJoinRoomResponse quickJoinRoomResponse = QuickJoinRoomResponse.newBuilder()
                .setRoomId(aliveRoom.getRoomId())
                .putAllMembersMap(aliveRoom.getMembersMap().values()
                        .stream()
                        .map(BeanConverter::member2RoomMember)
                        .collect(Collectors.toMap(RoomMember::getUid, Function.identity())))
                .build();
        pushService.push(userInfo.getUid(), SignalType.SIGNAL_TYPE_QUICK_JOIN_ROOM_RESPONSE, quickJoinRoomResponse);
        // 通知房间内的其他成员
//        pushService.batchPushSignal(new ArrayList<>(aliveRoom.getMembersMap().keySet()), xzcSignal);
        // 维护缓存
        userRelationCache.onJoinRoom(userInfo.getUid(), aliveRoom.getRoomId());
    }

    public void ready(UserInfo userInfo, ReadyRequest readyRequest) throws Exception {
        // 加入房间
        long roomId = readyRequest.getRoomId();
        AliveRoom aliveRoom = aliveRoomHolder.ready(roomId, userInfo, readyRequest.getReadyOrCancel());
        // 通知客户端
        ReadyResponse readyResponse = ReadyResponse.newBuilder()
                .setRoomId(roomId)
                .putAllMembersMap(aliveRoom.getMembersMap().values()
                        .stream()
                        .map(BeanConverter::member2RoomMember)
                        .collect(Collectors.toMap(RoomMember::getUid, Function.identity())))
                .build();
        // 通知房间内的其他成员
        pushService.batchPush(new ArrayList<>(aliveRoom.getMembersMap().keySet()),
                PushService.packSignalWithType(SignalType.SIGNAL_TYPE_READY_RESPONSE, readyResponse));
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
        pushService.push(userInfo.getUid(), SignalType.SIGNAL_TYPE_START_RESPONSE, startResponse);
        // todo 通知其他所有玩家
        userRelationCache.onStartGame(userInfo.getUid(), aliveGame.getId());
    }

    /**
     * 退出房间
     *
     * @param userInfo
     * @param roomId
     * @throws Exception
     */
    public void quit(UserInfo userInfo, Long roomId) throws Exception {
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
        pushService.push(userInfo.getUid(), SignalType.SIGNAL_TYPE_QUIT_RESPONSE, quitResponse);
        // todo 通知其他所有玩家

        userRelationCache.onQuitRoom(userInfo.getUid(), aliveRoom.getRoomId());
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
        quit(userInfo, roomId);
    }

    public AliveGame.Gamer userInfoToGamer(UserInfo userInfo) {
        return new AliveGame.Gamer()
                .setUid(userInfo.getUid())
                .setNickname(userInfo.getNickname())
                .setAvatar(userInfo.getAvatar());
    }

    public RoomMember userInfoToRoomMember(UserInfo userInfo) {
        return RoomMember.newBuilder()
                .setUid(userInfo.getUid())
                .setNickname(userInfo.getNickname())
                .setAvatar(userInfo.getAvatar())
                .build();
    }


}
