package xzc.server.service;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import xzc.server.bean.AliveGame;
import xzc.server.bean.UserInfo;
import xzc.server.proto.account.LoginRequest;
import xzc.server.proto.account.LoginResponse;
import xzc.server.proto.common.SignalType;
import xzc.server.websocket.WebsocketHolder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class AccountService {

    @Autowired
    private PushService pushService;

    @Autowired
    private UserRelationCache userRelationCache;

    @Autowired
    private AliveGameHolder aliveGameHolder;

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
                .setUserInfo(xzc.server.proto.account.UserInfo.newBuilder()
                        .setUid(userInfo.getUid())
                        .setNickname(userInfo.getNickname())
                        .setAvatar(userInfo.getAvatar())
                        .setBeans(userInfo.getBeans().doubleValue())
                        .build())
                .build();
        // 返回信息给客户端
        pushService.push(userInfo.getUid(), SignalType.SIGNAL_TYPE_LOGIN_RESPONSE, loginResponse);
        // todo 检查用户是否是中途掉线
        Map<Object, Object> userRelation = userRelationCache.getUserRelation(userInfo.getUid());
        if (!CollectionUtils.isEmpty(userRelation)) {
            Object gameId = userRelation.get(UserRelationCache.GAME_ID);
            if (gameId != null) {
                AliveGame aliveGame = aliveGameHolder.getAliveGame((Long) gameId);
                // TODO: 2022/3/13 判断游戏是否结束，如果没有结束就把当前的游戏进行状态信息发送给客户端。
//                aliveGame.get
            }
        }

    }


}
