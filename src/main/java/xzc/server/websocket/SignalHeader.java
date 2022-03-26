package xzc.server.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import xzc.server.constant.GameType;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class SignalHeader {
    //bit      byte      消息头的固定长度是69个字节
    // 8*2      2         包头起始标志，固定为“FC”
    private String flag = "SL";
    // 8*2      2         协议ID
    private short gameId = GameType.XZC.getId();
    // 8*4      4         协议格式类型，0为Protobuf格式，1为Json格式
    private int signal;
    // 8*1      1         协议格式类型，0为Protobuf格式，1为Json格式
    private byte formatType = 0;
    // 8*4      4         协议版本，用于迭代兼容, 目前填0
    private int protoVersion = 0;
    // 8*4      4         协议版本，用于迭代兼容, 目前填0
    private int clientVersion = 0;
    // 8*4      4         协议版本，用于迭代兼容, 目前填0
    private int serverVersion = 0;
    // 8*8      8         包序列号，用于对应请求包和回包, 要求递增或时间戳
    private long timestamp;
    // 8*4      4         包体长度
    private int bodySize;
    // 8*46     16        MD5哈希值
    private String md5 = "21d32f4f2e4a9406";
    // 8*20     20        保留20字节扩展
    private String exts = "34f32ffmkalo93jzs494";

}
