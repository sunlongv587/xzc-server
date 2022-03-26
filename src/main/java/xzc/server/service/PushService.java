package xzc.server.service;

import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xzc.server.proto.common.SignalType;
import xzc.server.websocket.SignalHeader;
import xzc.server.websocket.SignalMessage;
import xzc.server.websocket.WebsocketHolder;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PushService {

    public void push(Long uid, SignalMessage message) {
        Channel channel = WebsocketHolder.get(uid);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message);
        } else {
            log.warn("用户离线，uid: {}", uid);
        }
    }

    public void batchPush(List<Long> uids, SignalMessage message) {
        uids.forEach(uid -> push(uid, message));
    }

    public void push(Channel channel, SignalMessage signal) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(signal);
        } else {
            log.warn("用户离线，uid: {}", channel);
        }
    }

    public void push(Channel channel, SignalType signalType, GeneratedMessageV3 body) {
        push(channel, packSignalWithType(signalType, body));
    }

    public void push(Long receiver, SignalType signalType, GeneratedMessageV3 body) {
        push(receiver, packSignalWithType(signalType, body));
    }

    public static SignalMessage packSignalWithType(SignalType signalType, GeneratedMessageV3 body) {
        String uuid = UUID.randomUUID().toString();
        log.info("uuid: {}", uuid);
        byte[] bytes = body.toByteArray();

        return new SignalMessage(new SignalHeader()
                .setSignal(signalType.getNumber())
                .setTimestamp(System.currentTimeMillis())
                .setBodySize(bytes.length), bytes);

    }

}
