package xzc.server.service;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xzc.server.bean.UserInfo;
import xzc.server.proto.LoginRequest;
import xzc.server.proto.LoginResponse;
import xzc.server.websocket.WebsocketHolder;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Queue;

@Slf4j
@Service
public class AccountService {

    public static final Queue<UserInfo> TestUserQueue = new LinkedList<UserInfo>() {{
        push(new UserInfo()
                .setUid(1L)
                .setNickname("zhangsan")
                .setAvatar(null)
                .setBeans(new BigDecimal("100"))
        );
        push(new UserInfo()
                .setUid(2L)
                .setNickname("lisi")
                .setAvatar(null)
                .setBeans(new BigDecimal("100"))
        );
        push(new UserInfo()
                .setUid(3L)
                .setNickname("wangwu")
                .setAvatar(null)
                .setBeans(new BigDecimal("100"))
        );
        push(new UserInfo()
                .setUid(4L)
                .setNickname("zhaoliu")
                .setAvatar(null)
                .setBeans(new BigDecimal("100"))
        );
        push(new UserInfo()
                .setUid(5L)
                .setNickname("liyu")
                .setAvatar(null)
                .setBeans(new BigDecimal("100"))
        );
        push(new UserInfo()
                .setUid(6L)
                .setNickname("sunlong")
                .setAvatar(null)
                .setBeans(new BigDecimal("100"))
        );
    }};

    public void login(ChannelHandlerContext ctx, LoginRequest loginRequest) {
        // TODO: 2022/2/15 根据登录请求获取用户信息， 昵称，ID，头像，资产等
        // 先固定使用一个用户，测试
        UserInfo userInfo = TestUserQueue.poll();
        ctx.channel().attr(AttributeKey.valueOf("userInfo")).set(userInfo);
        // TODO: 2022/2/13 记录登录状态，将用户channel, 和用户信息记录到map中
        WebsocketHolder.put(userInfo.getUid(), ctx.channel());
        // TODO: 2022/2/13 返回登录响应
        LoginResponse loginResponse = LoginResponse.newBuilder()
                .setSuccess(true)
                .setUserInfo(xzc.server.proto.UserInfo.newBuilder()
                        .setUid(userInfo.getUid())
                        .setNickname(userInfo.getNickname())
                        .setAvatar(userInfo.getAvatar())
                        .setBeans(userInfo.getBeans().doubleValue())
                        .build())
                .build();
        // 返回信息给客户端
        ctx.channel().writeAndFlush(loginResponse);
    }


}
