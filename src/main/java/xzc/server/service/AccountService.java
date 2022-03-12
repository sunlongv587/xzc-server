package xzc.server.service;

import com.google.protobuf.Any;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xzc.server.bean.UserInfo;
import xzc.server.proto.LoginRequest;
import xzc.server.proto.LoginResponse;
import xzc.server.proto.XZCCommand;
import xzc.server.proto.XZCSignal;
import xzc.server.websocket.WebsocketHolder;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class AccountService {

    @Autowired
    private PushService pushService;

    public static volatile AtomicLong virtualUid = new AtomicLong(1);

    public void login(ChannelHandlerContext ctx, LoginRequest loginRequest) {
        // 根据登录请求获取用户信息， 昵称，ID，头像，资产等
        String username = loginRequest.getUsername();
        UserInfo userInfo = new UserInfo()
                .setNickname(username)
                .setAvatar("")
                .setBeans(new BigDecimal("10000"))
                .setUid(virtualUid.addAndGet(1));
        ctx.channel().attr(AttributeKey.valueOf("userInfo")).set(userInfo);
        // 记录登录状态，将用户channel, 和用户信息记录到map中
        WebsocketHolder.put(userInfo.getUid(), ctx.channel());
        // 返回登录响应
        LoginResponse loginResponse = LoginResponse.newBuilder()
                .setSuccess(true)
                .setUserInfo(xzc.server.proto.UserInfo.newBuilder()
                        .setUid(userInfo.getUid())
                        .setNickname(userInfo.getNickname())
                        .setAvatar(userInfo.getAvatar())
                        .setBeans(userInfo.getBeans().doubleValue())
                        .build())
                .build();
        // 封装成Signal
        XZCSignal xzcSignal = XZCSignal.newBuilder()
                .setCommand(XZCCommand.LOGIN_RESPONSE)
                .setBody(Any.pack(loginResponse))
                .build();
        // 返回信息给客户端
        pushService.pushSignal(userInfo.getUid(), xzcSignal);
    }


}
