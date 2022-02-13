package xzc.server.service;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Service;
import xzc.server.proto.LoginRequestBody;
import xzc.server.proto.SignalMessage;
import xzc.server.proto.XZCCommand;
import xzc.server.proto.XZCSignal;

import java.util.Map;

@Service
public class XzcSignalService {

    public void handleSignal(ChannelHandlerContext ctx, SignalMessage msg) throws InvalidProtocolBufferException {
        Any payload = msg.getPayload();
        if (payload.is(XZCSignal.class)) {
            XZCSignal signal = payload.unpack(XZCSignal.class);
            XZCCommand command = signal.getCommand();
            switch(command) {
                case LOGIN_REQUEST:
                    Any body = signal.getBody();
                    if (body.is(LoginRequestBody.class)) {
                        LoginRequestBody loginRequestBody = body.unpack(LoginRequestBody.class);
                        login(loginRequestBody);
                    }
                    break;
                case LOGIN_RESPONSE:
                    break;
                default:
                    break;
            }
        }
    }


    public void login(LoginRequestBody loginRequestBody) {
        // TODO: 2022/2/13 记录登录状态
        // TODO: 2022/2/13 返回登录响应

    }
}
