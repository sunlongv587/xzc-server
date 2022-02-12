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
        // 原包发回去
        ctx.channel().writeAndFlush(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if ("Connection reset by peer".equals(cause.getMessage())) {
            log.error("连接出现问题");
            return;
        }

        log.error(cause.getMessage(), cause);
    }

}
