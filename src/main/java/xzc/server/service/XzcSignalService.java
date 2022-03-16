package xzc.server.service;

import com.google.protobuf.Any;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xzc.server.bean.UserInfo;
import xzc.server.exception.XzcException;
import xzc.server.proto.common.*;
import xzc.server.proto.account.*;
import xzc.server.proto.game.*;
import xzc.server.proto.room.*;
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
            Any payload = msg.getPayload();
            if (payload.is(XzcSignal.class)) {
                XzcSignal signal = payload.unpack(XzcSignal.class);
                XzcCommand command = signal.getCommand();
                Any body = signal.getBody();
                UserInfo userInfo = (UserInfo) ctx.channel().attr(AttributeKey.valueOf("userInfo")).get();
                switch (command) {
                    case XZC_COMMAND_LOGIN_REQUEST:
                        if (body.is(LoginRequest.class)) {
                            LoginRequest loginRequest = body.unpack(LoginRequest.class);
                            accountService.login(ctx, loginRequest);
                        }
                        break;
                    case XZC_COMMAND_QUICK_JOIN_ROOM_REQUEST:
                        // 处理快速加入房间请求
                        if (body.is(QuickJoinRoomRequest.class)) {
                            QuickJoinRoomRequest quickJoinRoomRequest = body.unpack(QuickJoinRoomRequest.class);
                            roomService.quickJoin(userInfo, quickJoinRoomRequest);
                        }
                        break;
                    case XZC_COMMAND_READY_REQUEST:
                        // 处理准备请求
                        if (body.is(ReadyRequest.class)) {
                            ReadyRequest readyRequest = body.unpack(ReadyRequest.class);
                            roomService.ready(userInfo, readyRequest);
                        }
                        break;
                    case XZC_COMMAND_START_REQUEST:
                        // 处理开始请求
                        if (body.is(StartRequest.class)) {
                            StartRequest startRequest = body.unpack(StartRequest.class);
                            roomService.start(userInfo, startRequest);
                        }
                        break;
                    case XZC_COMMAND_QUIT_REQUEST:
                        // 处理退出请求
                        if (body.is(QuitRequest.class)) {
                            QuitRequest quitRequest = body.unpack(QuitRequest.class);
                            roomService.quit(userInfo, quitRequest);
                        }
                        break;
                    case XZC_COMMAND_QUICK_CHANGE_REQUEST:
                        // todo 快速更换房间
                        break;
                    case XZC_COMMAND_TAKE_CARD_REQUEST:
                        // 抓牌
                        if (body.is(TakeCardRequest.class)) {
                            TakeCardRequest takeCardRequest = body.unpack(TakeCardRequest.class);
                            gameService.takeCard(userInfo, takeCardRequest);
                        }
                        break;
                    case XZC_COMMAND_DISCARD_REQUEST:
                        // 弃牌
                        if (body.is(DiscardRequest.class)) {
                            DiscardRequest discardRequest = body.unpack(DiscardRequest.class);
                            gameService.discard(userInfo, discardRequest);
                        }
                        break;
                    case XZC_COMMAND_CHANGE_XZC_CARD_REQUEST:
                        // 更换小早川牌
                        if (body.is(ChangeXzcCardRequest.class)) {
                            ChangeXzcCardRequest changeXzcCardRequest = body.unpack(ChangeXzcCardRequest.class);
                            gameService.changeXzcCard(userInfo, changeXzcCardRequest);
                        }
                        break;

                    case XZC_COMMAND_LOGIN_RESPONSE:
                    case XZC_COMMAND_QUICK_JOIN_ROOM_RESPONSE:
                    case XZC_COMMAND_READY_RESPONSE:
                    case XZC_COMMAND_START_RESPONSE:
                    case XZC_COMMAND_QUIT_RESPONSE:
                    case XZC_COMMAND_QUICK_CHANGE_RESPONSE:
                    case XZC_COMMAND_TAKE_CARD_RESPONSE:
                    case XZC_COMMAND_DISCARD_RESPONSE:
                    case XZC_COMMAND_CHANGE_XZC_CARD_RESPONSE:
                        log.warn("Response command: {}, Ignore handle", command.name());
                        break;

                    default:
                        log.warn("Unknown command: {}, Ignore handle", command.name());
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
            XzcSignal xzcSignal = XzcSignal.newBuilder()
                    .setCommand(XzcCommand.XZC_COMMAND_ERROR_RESPONSE)
                    .setBody(Any.pack(builder.build()))
                    .build();
            pushService.pushSignal(ctx.channel(), xzcSignal);
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
