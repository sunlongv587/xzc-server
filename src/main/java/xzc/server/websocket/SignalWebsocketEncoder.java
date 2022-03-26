package xzc.server.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.List;

public class SignalWebsocketEncoder extends MessageToMessageEncoder<SignalMessage> {


    @Override
    protected void encode(ChannelHandlerContext ctx, SignalMessage signalMessage, List<Object> out) throws Exception {

        // 将Message转换成二进制数据
        SignalHeader header = signalMessage.getHeader();

        // 这里写入的顺序就是协议的顺序.

        ByteBuf result = Unpooled.buffer();

        // 写入Header信息
        result.writeBytes(header.getFlag().getBytes());
        result.writeInt(header.getGameId());
        result.writeInt(header.getSignal());
        result.writeInt(header.getFormatType());
        result.writeInt(header.getProtoVersion());
        result.writeInt(header.getClientVersion());
        result.writeInt(header.getServerVersion());

        result.writeLong(header.getTimestamp());
        result.writeInt(signalMessage.getBody().length);
        // todo 签名规则还没定，先保留吧
        result.writeBytes(header.getMd5().getBytes());
        result.writeBytes(header.getExts().getBytes());

        // 写入消息主体信息
        result.writeBytes(signalMessage.getBody());

        // 然后下面再转成websocket二进制流，因为客户端不能直接解析protobuf编码生成的
        WebSocketFrame frame = new BinaryWebSocketFrame(result);
        out.add(frame);
    }
}
