package xzc.server.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.nio.ByteBuffer;

@Getter
@Setter
@Accessors(chain = true)
@AllArgsConstructor
public class SignalMessage {

    private SignalHeader header;

    private byte[] body;

//
//    /**
//     *
//     * @param nProtoID
//     * @param body
//     */
//    public SignalMessage(int nProtoID, byte[] body){
//        String nHeaderFlag = "FC";//包头起始标志，固定为“FC”
//
//        int nProtoFmtType = 0; //协议格式类型，0为Protobuf格式，1为Json格式
//
//        int nProtoVer = 0;//协议版本，用于迭代兼容, 目前填0
//
//        long nSerialNo = System.currentTimeMillis();//包序列号，用于对应请求包和回包, 要求递增或时间戳。这里填时间戳
//
//        int nBodyLen = body.length;//包体长度
//
//        String arrBodySHA1 = "2cef026959a7f224bbf001874e2d955a3863a2fe";//包体原始数据(解密后)的SHA1哈希值
//
//        String arrReserved = "a234567890123456789z";//保留20字节扩展
//
//        // 组装协议头
//        SignalHeader header = new SignalHeader(nHeaderFlag, nProtoID,  nProtoFmtType,  nProtoVer,  nSerialNo,  nBodyLen,  arrBodySHA1,  arrReserved);
//
//        this.header = header;
//        this.body = body;
//    }
//
//    public SignalMessage(ByteBuffer buffer){
//        byte[] headFlagBytes = new byte[2];
//        buffer.get(headFlagBytes);
//        String nHeaderFlag = new String(headFlagBytes);
//
//        int nProtoID = buffer.getInt();
//
//        int nProtoFmtType = buffer.getInt();
//
//        int nProtoVer = buffer.getInt();
//
//        long nSerialNo = buffer.getLong();
//
//        int nBodyLen = buffer.getInt();
//
//        byte[] sha1Bytes = new byte[40];
//        buffer.get(sha1Bytes);
//        String arrBodySHA1 = new String(sha1Bytes);
//
//        byte[] reservedBytes = new byte[20];
//        buffer.get(reservedBytes);
//        String arrReserved = new String(reservedBytes);
//
//        // 读取消息内容
//        byte[] bodyBytes = new byte[nBodyLen];
//        buffer.get(bodyBytes);
//
//        SignalHeader header = new SignalHeader(nHeaderFlag, nProtoID,  nProtoFmtType,  nProtoVer,  nSerialNo,  nBodyLen,  arrBodySHA1,  arrReserved);
//
//        this.header = header;
//        this.body = bodyBytes;
//    }

    /**
     * @return
     */
    public ByteBuffer toByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(69 + this.header.getBodySize());
        buffer.put(header.getFlag().getBytes());
        buffer.putInt(header.getGameId());
        buffer.putInt(header.getFormatType());
        buffer.putInt(header.getClientVersion());

        buffer.putLong(header.getTimestamp());
        buffer.putInt(getBody().length);

        buffer.put(header.getMd5().getBytes());
        buffer.put(header.getExts().getBytes());
        buffer.put(getBody());

        buffer.flip();

        return buffer;
    }
}
