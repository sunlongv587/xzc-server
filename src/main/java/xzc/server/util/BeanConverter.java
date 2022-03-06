package xzc.server.util;

import xzc.server.bean.AliveRoom;
import xzc.server.proto.Participant;

public class BeanConverter {
    public static Participant member2Participant(AliveRoom.Member member) {
        return Participant
                .newBuilder()
                .setUid(member.getUid())
                .setNickname(member.getNickname())
                .setAvatar(member.getAvatar())
                .setEvent(AliveRoom.MemberEvent.toParticipantEvent(member.getEvent()))
                .setState(AliveRoom.MemberState.toParticipantState(member.getState()))
                .build();
    }
}
