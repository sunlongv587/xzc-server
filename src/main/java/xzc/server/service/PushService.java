package xzc.server.service;

import com.google.protobuf.Any;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xzc.server.proto.MessageType;
import xzc.server.proto.SignalMessage;
import xzc.server.proto.XzcSignal;
import xzc.server.websocket.WebsocketHolder;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PushService {

    public void pushMessage(Long uid, SignalMessage message) {
        Channel channel = WebsocketHolder.get(uid);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message);
        } else {
            log.warn("用户离线，uid: {}", uid);
        }
    }

    public void batchPushMessage(List<Long> uids, SignalMessage message) {
        uids.forEach(uid -> pushMessage(uid, message));
    }

    public void batchPush(List<Long> receivers, MessageType messageType, XzcSignal signal) {
        receivers.forEach(receiver -> push(receiver, messageType, signal));
    }

    public void batchPushSignal(List<Long> receivers, XzcSignal signal) {
        receivers.forEach(receiver -> pushSignal(receiver, signal));
    }

    public void pushSignal(Long receiver, XzcSignal signal) {
        push(receiver, MessageType.SIGNAL, signal);
    }

    public void pushSignal(Channel channel, XzcSignal signal) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(packSignal(signal));
        } else {
            log.warn("用户离线，uid: {}", channel);
        }
    }

    public void push(Long receiver, MessageType messageType, XzcSignal signal) {
        pushMessage(receiver, packSignalWithType(messageType, signal));
    }

    public SignalMessage packSignal(XzcSignal signal) {
        String uuid = UUID.randomUUID().toString();
        log.info("uuid: {}", uuid);
        return SignalMessage.newBuilder()
                .setGameId(1)
                .setTimestamp(System.currentTimeMillis())
                .setMessageId(uuid)
                .setType(MessageType.SIGNAL)
                .setVersion("1.0")
                .setPayload(Any.pack(signal))
                .build();
    }

    public SignalMessage packSignalWithType(MessageType messageType, XzcSignal signal) {
        String uuid = UUID.randomUUID().toString();
        log.info("uuid: {}", uuid);
        return SignalMessage.newBuilder()
                .setGameId(1)
                .setTimestamp(System.currentTimeMillis())
                .setMessageId(uuid)
                .setType(messageType)
                .setVersion("1.0")
                .setPayload(Any.pack(signal))
                .build();
    }

}
