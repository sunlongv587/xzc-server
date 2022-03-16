package xzc.server.websocket;

import com.google.protobuf.Any;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import xzc.server.proto.common.MessageType;
import xzc.server.proto.common.SignalMessage;
import xzc.server.service.XzcSignalService;

@ChannelHandler.Sharable
@Slf4j
public class WebsocketServerHandler extends SimpleChannelInboundHandler<SignalMessage> {

    private XzcSignalService xzcSignalService;

    public WebsocketServerHandler setXzcSignalService(XzcSignalService xzcSignalService) {
        this.xzcSignalService = xzcSignalService;
        return this;
    }

    /**
     * 取消绑定
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 可能出现业务判断离线后再次触发 channelInactive
        log.warn("触发 channelInactive 掉线![{}]", ctx.channel().id());
        xzcSignalService.handleOffline(ctx);
    }

    /**
     * 心跳检查
     *
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
                xzcSignalService.handleOffline(ctx);
            }
        }
        super.userEventTriggered(ctx, evt);
    }


    /**
     * 读到客户端的内容
     *
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
        Any payload = msg.getPayload();
        switch (type) {
            case MESSAGE_TYPE_ECHO: // 心跳
                log.info("客户端echo");
                echo(ctx, msg);
                break;
            case MESSAGE_TYPE_SIGNAL: // 处理信令
                log.info("客户端信令signal");
                if (gameId == 1) {
                    xzcSignalService.handleSignal(ctx, msg);
                }
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
