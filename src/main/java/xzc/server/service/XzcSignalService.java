package xzc.server.service;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
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


    public void handleSignal(ChannelHandlerContext ctx, SignalMessage msg) throws InvalidProtocolBufferException {

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
                case LOGIN_RESPONSE:
                case QUICK_JOIN_ROOM_RESPONSE:
                    log.warn("忽略处理");
                    break;

                case QUICK_JOIN_ROOM_REQUEST:
                    // TODO: 2022/2/15 处理快速加入房间请求
//                    Any body = signal.getBody();
                    if (body.is(QuickJoinRoomRequest.class)) {
                        QuickJoinRoomRequest quickJoinRoomRequest = body.unpack(QuickJoinRoomRequest.class);
//                        login(ctx, loginRequest);
                        UserInfo userInfo = (UserInfo) ctx.channel().attr(AttributeKey.valueOf("userInfo")).get();
                        roomService.quickJoin(userInfo, quickJoinRoomRequest);
                    }

                default:
                    break;
            }
        }
    }




}
