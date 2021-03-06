package xzc.server.constant;

import org.apache.commons.lang3.StringUtils;

public class RedisKey {

    public static final String SEPARATOR = ":";

    public static final String ALIVE_ROOM = "alive-room";

    public static final String ALIVE_ROOM_LOCK = "alive-room:lock";

    public static final String USER_RELATION = "user:relation";

    /**
     * zset member = roomId, score = 剩余座位数（Remaining seats）
     */
    public static final String OPTIONAL_ROOMS = "optional-rooms";

    public static final String GAME = "alive-game";

    public static final String GAME_LOCK = "alive-game:lock";

    public static String makeKey(Object... tokens) {

        if (tokens != null && tokens.length > 0) {
            return StringUtils.join(tokens, SEPARATOR);
        } else {
            return null;
        }
    }

}
