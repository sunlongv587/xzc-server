package xzc.server.service;

import com.google.protobuf.Any;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xzc.server.proto.MessageType;
import xzc.server.proto.SignalMessage;
import xzc.server.proto.XZCSignal;
import xzc.server.websocket.WebsocketHolder;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PushService {

    public void pushMessage(Long uid, SignalMessage message) {
        Channel channel = WebsocketHolder.get(uid);
        channel.writeAndFlush(message);
    }

    public void batchPushMessage(List<Long> uids, SignalMessage message) {
        uids.forEach(uid -> pushMessage(uid, message));
    }

    public void batchPush(List<Long> receivers, MessageType messageType, XZCSignal signal) {
        receivers.forEach(receiver -> push(receiver, messageType, signal));
    }

    public void batchPushSignal(List<Long> receivers, XZCSignal signal) {
        receivers.forEach(receiver -> pushSignal(receiver, signal));
    }

    public void pushSignal(Long receiver, XZCSignal signal) {
        push(receiver, MessageType.SIGNAL, signal);
    }

    public void push(Long receiver, MessageType messageType, XZCSignal signal) {
        String uuid = UUID.randomUUID().toString();
        System.out.println("uuid: " + uuid);
        SignalMessage signalMessage = SignalMessage.newBuilder()
                .setGameId(1)
                .setTimestamp(System.currentTimeMillis())
                .setMessageId(uuid)
                .setType(messageType)
                .setVersion("1.0")
                .setPayload(Any.pack(signal))
                .build();
        pushMessage(receiver, signalMessage);
    }

}
