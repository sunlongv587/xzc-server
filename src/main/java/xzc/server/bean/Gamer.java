package xzc.server.bean;


import lombok.Data;
import lombok.experimental.Accessors;
import xzc.server.proto.ParticipantEvent;
import xzc.server.proto.ParticipantState;

@Data
@Accessors(chain = true)
public class Gamer {

    private Long uid;

    private String nickname;

    private String avatar;

    private ParticipantState state;

    private ParticipantEvent event;

    private Long joinTime;

    private Long lastStateChange;
}
