package xzc.server.bean;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xzc.server.constant.RoomState;
import xzc.server.proto.ParticipantEvent;
import xzc.server.proto.ParticipantState;
import xzc.server.proto.RoomType;

import java.util.LinkedHashMap;

@Data
@Accessors(chain = true)
public class AliveRoom {

    private Long roomId;

    private RoomType roomType;

    private int minPlayNum = 3;

    private int maxPlayNum = 6;

    private Integer joinedIncr = 0;

    private RoomState state = RoomState.OPENED;

    private LinkedHashMap<Long, Member> membersMap;

    private Long lastChangeTime;

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Member {
        private Long uid;

        private Integer index;

        private String nickname;

        private String avatar;

        private MemberState state;

        private MemberEvent event;

        private Long joinTime;

        private Long lastStateChange;
    }

    public enum MemberState {
        IDLE,
        PLAYING,
        IN_PLAY;

        public static ParticipantState toParticipantState(MemberState memberState) {
            switch (memberState) {
                case IDLE:
                    return ParticipantState.IDLE;
                case IN_PLAY:
                    return ParticipantState.IN_PLAY;
                case PLAYING:
                    return ParticipantState.PLAYING;
                default:
                    return ParticipantState.UNRECOGNIZED;
            }
        }
    }

    public enum MemberEvent {
        NONE,
        JOIN,
        READY,
        CANCEL_READY;

        public static ParticipantEvent toParticipantEvent(MemberEvent memberState) {
            switch (memberState) {
                case NONE:
                    return ParticipantEvent.NONE;
                case JOIN:
                    return ParticipantEvent.JOIN;
                case READY:
                    return ParticipantEvent.READY;
                case CANCEL_READY:
                    return ParticipantEvent.CANCEL_READY;
                default:
                    return ParticipantEvent.UNRECOGNIZED;
            }
        }
    }
}
