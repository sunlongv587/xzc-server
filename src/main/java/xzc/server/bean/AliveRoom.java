package xzc.server.bean;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xzc.server.constant.RoomState;
import xzc.server.proto.room.MemberEvent;
import xzc.server.proto.room.MemberState;
import xzc.server.proto.room.RoomType;

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

        public static xzc.server.proto.room.MemberState toRoomMemberState(MemberState memberState) {
            switch (memberState) {
                case IDLE:
                    return xzc.server.proto.room.MemberState.MEMBER_STATE_IDLE;
                case IN_PLAY:
                    return xzc.server.proto.room.MemberState.MEMBER_STATE_IN_PLAY;
                case PLAYING:
                    return xzc.server.proto.room.MemberState.MEMBER_STATE_PLAYING;
                default:
                    return xzc.server.proto.room.MemberState.MEMBER_STATE_UNSPECIFIED;
            }
        }
    }

    public enum MemberEvent {
        NONE,
        JOIN,
        READY,
        CANCEL_READY,
        START,
        QUIT;

        public static xzc.server.proto.room.MemberEvent toRoomMemberEvent(MemberEvent memberState) {
            switch (memberState) {
                case NONE:
                    return xzc.server.proto.room.MemberEvent.MEMBER_EVENT_UNSPECIFIED;
                case JOIN:
                    return xzc.server.proto.room.MemberEvent.MEMBER_EVENT_JOIN;
                case READY:
                    return xzc.server.proto.room.MemberEvent.MEMBER_EVENT_READY;
                case CANCEL_READY:
                    return xzc.server.proto.room.MemberEvent.MEMBER_EVENT_CANCEL_READY;
                case QUIT:
                    return xzc.server.proto.room.MemberEvent.MEMBER_EVENT_QUIT;
                default:
                    return xzc.server.proto.room.MemberEvent.UNRECOGNIZED;
            }
        }
    }
}
