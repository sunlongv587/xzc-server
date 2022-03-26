package xzc.server.websocket;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;


@Slf4j
//@Component
public class WebsocketServerInitializer extends ChannelInitializer<SocketChannel> {

    //    @Autowired
    private ChannelInboundHandler channelInboundHandler;

    private String path = "/ws";

    public WebsocketServerInitializer(ChannelInboundHandler channelInboundHandler) {
        this.channelInboundHandler = channelInboundHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 60秒客户端没有向服务器发送心跳则关闭连接
        pipeline.addLast(new IdleStateHandler(600, 0, 0));
        // websocket协议本身是基于http协议的，所以这边也要使用http解编码器
        pipeline.addLast(new HttpServerCodec());
        // 主要用于处理大数据流，比如一个1G大小的文件如果你直接传输肯定会撑暴jvm内存的; 增加之后就不用考虑这个问题了
        pipeline.addLast(new ChunkedWriteHandler());
        // netty是基于分段请求的，HttpObjectAggregator的作用是将请求分段再聚合,参数是聚合字节的最大长度
        pipeline.addLast(new HttpObjectAggregator(1024 * 1024 * 1024));

        // WebSocket数据压缩
        pipeline.addLast(new WebSocketServerCompressionHandler());
        // 协议包长度限制
        pipeline.addLast(new WebSocketServerProtocolHandler(path, null, true));
        // 协议包解码时指定Protobuf字节数实例化为CommonProtocol类型
//        pipeline.addLast(new ProtobufDecoder(SignalMessage.getDefaultInstance()));
        pipeline.addLast(new SignalWebsocketEncoder());
        pipeline.addLast(new SignalWebsocketDecoder());

        // websocket定义了传递数据的6中frame类型
        pipeline.addLast(channelInboundHandler);
    }

}
