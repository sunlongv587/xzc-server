package xzc.server.service;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xzc.server.websocket.WebsocketHolder;

import java.util.List;

@Slf4j
@Service
public class PushService {

    public void pushMessage(Long uid, Object message) {
        Channel channel = WebsocketHolder.get(uid);
        channel.writeAndFlush(message);
    }

    public void batchPushMessage(List<Long> uids, Object message) {
        uids.forEach(uid -> pushMessage(uid, message));
    }
}
