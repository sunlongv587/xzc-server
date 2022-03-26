package xzc.server.constant;

import lombok.Getter;

@Getter
public enum GameType {

    XZC((short) 1);

    short id;

    GameType(short id) {
        this.id = id;
    }
}
