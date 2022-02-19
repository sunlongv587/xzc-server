package xzc.server.bean;

import lombok.Data;
import lombok.experimental.Accessors;
import xzc.server.constant.RoomState;
import xzc.server.proto.RoomType;

import java.util.LinkedHashMap;

@Data
@Accessors(chain = true)
public class AliveRoom {

    public Long roomId;

    public RoomType roomType;

    public int minPlayNum = 3;

    public int maxPlayNum = 6;

    public RoomState state = RoomState.OPENED;

    public LinkedHashMap<Long, Gamer> gamerMap;


}
