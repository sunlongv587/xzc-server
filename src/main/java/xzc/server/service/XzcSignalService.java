package xzc.server.service;

import com.google.protobuf.Any;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xzc.server.bean.UserInfo;
import xzc.server.proto.*;


@Slf4j
@Service
public class XzcSignalService {

    @Autowired
    private AccountService accountService;

    @Autowired
    private RoomService roomService;

    public void handleSignal(ChannelHandlerContext ctx, SignalMessage msg) throws Exception {

        try {
            Any payload = msg.getPayload();
            if (payload.is(XZCSignal.class)) {
                XZCSignal signal = payload.unpack(XZCSignal.class);
                XZCCommand command = signal.getCommand();
                Any body = signal.getBody();
                switch(command) {
                    case LOGIN_REQUEST:
                        if (body.is(LoginRequest.class)) {
                            LoginRequest loginRequest = body.unpack(LoginRequest.class);
                            accountService.login(ctx, loginRequest);
                        }
                        break;
                    case QUICK_JOIN_ROOM_REQUEST:
                        // 处理快速加入房间请求
                        if (body.is(QuickJoinRoomRequest.class)) {
                            QuickJoinRoomRequest quickJoinRoomRequest = body.unpack(QuickJoinRoomRequest.class);
                            UserInfo userInfo = (UserInfo) ctx.channel().attr(AttributeKey.valueOf("userInfo")).get();
                            roomService.quickJoin(userInfo, quickJoinRoomRequest);
                        }
                    case READY_REQUEST:
                        // 处理准备请求
                        if (body.is(ReadyRequest.class)) {
                            ReadyRequest readyRequest = body.unpack(ReadyRequest.class);
                            UserInfo userInfo = (UserInfo) ctx.channel().attr(AttributeKey.valueOf("userInfo")).get();
                            roomService.ready(userInfo, readyRequest);
                        }
                    case START_REQUEST:
                        // 处理开始请求
                        if (body.is(StartRequest.class)) {
                            StartRequest startRequest = body.unpack(StartRequest.class);
                            UserInfo userInfo = (UserInfo) ctx.channel().attr(AttributeKey.valueOf("userInfo")).get();
                            roomService.start(userInfo, startRequest);
                        }
                    case QUIT_REQUEST:
                        // 处理退出请求
                        if (body.is(QuitRequest.class)) {
                            QuitRequest quitRequest = body.unpack(QuitRequest.class);
                            UserInfo userInfo = (UserInfo) ctx.channel().attr(AttributeKey.valueOf("userInfo")).get();
                            roomService.quit(userInfo, quitRequest);
                        }

                    case LOGIN_RESPONSE:
                    case QUICK_JOIN_ROOM_RESPONSE:
                    case READY_RESPONSE:
                    case START_RESPONSE:
                        log.warn("忽略处理");
                        break;

              default:
                        break;
                }
            }
        } catch (Exception e) {
            log.warn("处理信令异常，", e);
        } finally {
        }
    }




}
