package xzc.server.util;

import xzc.server.bean.AliveRoom;
import xzc.server.proto.RoomMember;

public class BeanConverter {
    public static RoomMember member2RoomMember(AliveRoom.Member member) {
        return RoomMember
                .newBuilder()
                .setUid(member.getUid())
                .setNickname(member.getNickname())
                .setAvatar(member.getAvatar())
                .setEvent(AliveRoom.MemberEvent.toRoomMemberEvent(member.getEvent()))
                .setState(AliveRoom.MemberState.toRoomMemberState(member.getState()))
                .build();
    }
}
