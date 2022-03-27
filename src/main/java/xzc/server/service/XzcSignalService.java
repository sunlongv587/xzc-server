package xzc.server.service;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xzc.server.bean.UserInfo;
import xzc.server.constant.GameType;
import xzc.server.exception.XzcException;
import xzc.server.proto.account.LoginRequest;
import xzc.server.proto.common.ErrorResponse;
import xzc.server.proto.common.SignalType;
import xzc.server.proto.game.ChangeXzcCardRequest;
import xzc.server.proto.game.DiscardRequest;
import xzc.server.proto.game.TakeCardRequest;
import xzc.server.proto.room.QuickJoinRoomRequest;
import xzc.server.proto.room.QuitRequest;
import xzc.server.proto.room.ReadyRequest;
import xzc.server.proto.room.StartRequest;
import xzc.server.websocket.SignalHeader;
import xzc.server.websocket.SignalMessage;
import xzc.server.websocket.WebsocketHolder;

import java.util.Map;


@Slf4j
@Service
public class XzcSignalService {

    @Autowired
    private AccountService accountService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private PushService pushService;

    @Autowired
    private GameService gameService;

    @Autowired
    private UserRelationCache userRelationCache;

    public void handleSignal(ChannelHandlerContext ctx, SignalMessage msg) throws Exception {

        try {
            SignalHeader header = msg.getHeader();
            byte[] body = msg.getBody();
            if (header.getGameId() == GameType.XZC.getId()) {
                int signal = header.getSignal();
                SignalType signalType = SignalType.forNumber(signal);
                UserInfo userInfo = (UserInfo) ctx.channel().attr(AttributeKey.valueOf("userInfo")).get();
                switch (signalType) {
                    case SIGNAL_TYPE_LOGIN_REQUEST:
                        LoginRequest loginRequest = LoginRequest.parseFrom(body);
                        accountService.login(ctx, loginRequest);
                        break;
                    case SIGNAL_TYPE_QUICK_JOIN_ROOM_REQUEST:
                        // 处理快速加入房间请求
                        QuickJoinRoomRequest quickJoinRoomRequest = QuickJoinRoomRequest.parseFrom(body);
                        roomService.quickJoin(userInfo, quickJoinRoomRequest);
                        break;
                    case SIGNAL_TYPE_READY_REQUEST:
                        // 处理准备请求
                        ReadyRequest readyRequest = ReadyRequest.parseFrom(body);
                        roomService.ready(userInfo, readyRequest);
                        break;
                    case SIGNAL_TYPE_START_REQUEST:
                        // 处理开始请求
                            StartRequest startRequest = StartRequest.parseFrom(body);
                            roomService.start(userInfo, startRequest);
                        break;
                    case SIGNAL_TYPE_QUIT_REQUEST:
                        // 处理退出请求
                            QuitRequest quitRequest = QuitRequest.parseFrom(body);
                            roomService.quit(userInfo, quitRequest);
                        break;
                    case SIGNAL_TYPE_QUICK_CHANGE_REQUEST:
                        // todo 快速更换房间
                        break;
                    case SIGNAL_TYPE_TAKE_CARD_REQUEST:
                        // 抓牌
                            TakeCardRequest takeCardRequest = TakeCardRequest.parseFrom(body);
                            gameService.takeCard(userInfo, takeCardRequest);
                        break;
                    case SIGNAL_TYPE_DISCARD_REQUEST:
                        // 弃牌
                            DiscardRequest discardRequest = DiscardRequest.parseFrom(body);
                            gameService.discard(userInfo, discardRequest);
                        break;
                    case SIGNAL_TYPE_CHANGE_XZC_CARD_REQUEST:
                        // 更换小早川牌
                            ChangeXzcCardRequest changeXzcCardRequest = ChangeXzcCardRequest.parseFrom(body);
                            gameService.changeXzcCard(userInfo, changeXzcCardRequest);
                        break;

                    case SIGNAL_TYPE_LOGIN_RESPONSE:
                    case SIGNAL_TYPE_QUICK_JOIN_ROOM_RESPONSE:
                    case SIGNAL_TYPE_READY_RESPONSE:
                    case SIGNAL_TYPE_START_RESPONSE:
                    case SIGNAL_TYPE_QUIT_RESPONSE:
                    case SIGNAL_TYPE_QUICK_CHANGE_RESPONSE:
                    case SIGNAL_TYPE_TAKE_CARD_RESPONSE:
                    case SIGNAL_TYPE_DISCARD_RESPONSE:
                    case SIGNAL_TYPE_CHANGE_XZC_CARD_RESPONSE:
                        log.warn("Response signal: {}, Ignore handle", signalType.name());
                        break;

                    default:
                        log.warn("Unknown signal: {}, Ignore handle", signalType.name());
                        break;
                }
            }
        } catch (XzcException exception) {
            log.warn("处理信令异常，", exception);
            Map<String, String> data = exception.getData();
            ErrorResponse.Builder builder = ErrorResponse.newBuilder()
                    .setCode(exception.getErrorCode())
                    .setMessage(exception.getMessage());
            if (data != null) {
                builder.putAllData(data);
            }
            pushService.push(ctx.channel(), SignalType.SIGNAL_TYPE_ERROR_RESPONSE, builder.build());
        } catch (Exception e) {
            log.warn("处理信令异常，", e);
        } finally {
        }
    }

    public void handleOffline(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        UserInfo userInfo = (UserInfo) ctx.channel().attr(AttributeKey.valueOf("userInfo")).get();
        if (userInfo != null) {
            long uid = userInfo.getUid();
            WebsocketHolder.removeById(uid);
            // 查询用户加入的房间，将用户从房间中移除
            Map<Object, Object> userRelation = userRelationCache.getUserRelation(uid);
            Object roomId = userRelation.get(UserRelationCache.ROOM_ID);
            // 退出该房间
            roomService.quit(userInfo, (Long) roomId);
            // todo 加入的游戏中改为托管状态
//            gameService.
        } else {
            WebsocketHolder.remove(channel);
        }
        channel.close();
    }


}
