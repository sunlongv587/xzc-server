package xzc.server.websocket;

import com.google.protobuf.Any;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import xzc.server.proto.MessageType;
import xzc.server.proto.SignalMessage;

import java.io.IOException;
import java.util.Map;

@ChannelHandler.Sharable
@Slf4j
public class WebsocketServerHandler extends SimpleChannelInboundHandler<SignalMessage> {

    /**
     * 取消绑定
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 可能出现业务判断离线后再次触发 channelInactive
        log.warn("触发 channelInactive 掉线![{}]", ctx.channel().id());
        userOffLine(ctx);
    }

    /**
     * 心跳检查
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            // 读空闲
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                // 关闭用户的连接
                userOffLine(ctx);
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    /**
     * 用户下线
     */
    private void userOffLine(ChannelHandlerContext ctx) throws IOException {
        WebsocketHolder.remove(ctx.channel());
        ctx.channel().close();
    }

    /**
     * 读到客户端的内容 （这里只做心跳检查）
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SignalMessage msg) throws Exception {
        String messageId = msg.getMessageId();
        int gameId = msg.getGameId();
        MessageType type = msg.getType();
        long timestamp = msg.getTimestamp();
        Map<String, Any> payloadMap = msg.getPayloadMap();
        switch (type) {
            case ECHO: // 心跳
                log.info("客户端echo");
                echo(ctx, msg);
                break;
            case SIGNAL: // 处理信令
                log.info("客户端信令");
                break;
            default:
                log.info("未知类型");
        }

    }

    private void echo(ChannelHandlerContext ctx, SignalMessage msg) {
        log.info("echo");
    }
//
//    private void userLogin(ChannelHandlerContext ctx, Long uid, String sid) throws IOException {
//        if (!UserLoginUtils.checkToken(uid, sid)) {
//            log.info("非法登录: {}, {}", uid, sid);
//            // 登录异常, 发送下线通知
//            WSBaseResProtoOuterClass.WSBaseResProto wsBaseResProto = WSBaseResProtoOuterClass.WSBaseResProto.newBuilder()
//                    .setType(WSResTypeConstant.LOGIN_OUT)
//                    .setCreateTime(new Date().toString())
//                    .build();
//            // 发送下线消息
//            ctx.channel().writeAndFlush(wsBaseResProto);
//            return;
//        }
//
//        // 判断是否在线, 如果在线, 则剔除当前在线用户
//        Channel channel = WSSocketHolder.get(uid);
//        // 如果不是第一次登陆, 并且 客户端ID和当前的不匹配, 则通知之前的客户端下线
//        if (channel != null && !ctx.channel().id().equals(channel.id())) {
//            WSBaseResProtoOuterClass.WSBaseResProto wsBaseResProto = WSBaseResProtoOuterClass.WSBaseResProto.newBuilder()
//                    .setType(WSResTypeConstant.WS_OUT)
//                    .setCreateTime(new Date().toString())
//                    .build();
//            // 发送下线消息
//            channel.writeAndFlush(wsBaseResProto);
//        }
//
//        // 加入 在线 map 中
//        WSSocketHolder.put(uid, ctx.channel());
//    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if ("Connection reset by peer".equals(cause.getMessage())) {
            log.error("连接出现问题");
            return;
        }

        log.error(cause.getMessage(), cause);
    }

}
