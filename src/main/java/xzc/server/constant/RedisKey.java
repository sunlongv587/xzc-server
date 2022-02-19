package xzc.server.constant;

import org.apache.commons.lang3.StringUtils;

public class RedisKey {

    public static final String SEPARATOR = ":";

    public static final String GAME = "game";

    public static final String GAME_LOCK = "game:lock";

    public static String makeKey(Object... tokens) {

        if (tokens != null && tokens.length > 0) {
            return StringUtils.join(tokens, SEPARATOR);
        } else {
            return null;
        }
    }

}
