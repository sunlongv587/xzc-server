package xzc.server.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.List;

public class SignalWebsocketDecoder extends MessageToMessageDecoder<WebSocketFrame> {

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
        ByteBuf in = frame.content();
        int size = in.readableBytes();
//        System.out.println("可读长度："+size); //可读长度

        in.markReaderIndex();//从头开始读取

        byte[] flagBytes = new byte[2];
        in.readBytes(flagBytes);
        String flag = new String(flagBytes);

        short gameId = in.readShort();
        int signal = in.readInt();
        byte formatType = in.readByte();
        int protoVersion = in.readInt();
        int clientVersion = in.readInt();
        int serverVersion = in.readInt();
        long timestamp = in.readLong();
        int bodySize = in.readInt();

        byte[] md5Bytes = new byte[16];
        in.readBytes(md5Bytes);
        String md5 = new String(md5Bytes);

        byte[] extsBytes = new byte[20];
        in.readBytes(extsBytes);
        String exts = new String(extsBytes);

        // 组装协议头
        SignalHeader header = new SignalHeader(flag, gameId, signal, formatType, protoVersion, clientVersion, serverVersion,
                timestamp, bodySize, md5, exts);

        // 读取消息内容
        byte[] bodyBytes = new byte[bodySize];
        in.readBytes(bodyBytes);

        SignalMessage message = new SignalMessage(header, bodyBytes);
        out.add(message);
    }

}
