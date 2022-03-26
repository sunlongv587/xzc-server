package xzc.server.websocket;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import xzc.server.proto.common.SignalType;
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
        log.info("userEventTriggered sid:{},ip:{}", ctx.channel().id(), ctx.channel().remoteAddress());
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
     * @param signalMessage
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SignalMessage signalMessage) throws Exception {
        int gameId = signalMessage.getHeader().getGameId();
        int signal = signalMessage.getHeader().getSignal();
        SignalType signalType = SignalType.forNumber(signal);
        long timestamp = signalMessage.getHeader().getTimestamp();
//        Any payload = signalMessage.getPayload();
        switch (signalType) {
            case SIGNAL_TYPE_BETTING_REQUEST: // 心跳
                log.info("客户端echo");
                echo(ctx, signalMessage);
                break;
            default:
                xzcSignalService.handleSignal(ctx, signalMessage);
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
