package com.github.observant_sun.rattlegram.entity;

import lombok.Getter;

@Getter
public enum MessageType {
    NORMAL_INCOMING(Direction.INCOMING, false),
    PING_INCOMING(Direction.INCOMING, false),
    ERROR_INCOMING(Direction.INCOMING, true),
    NORMAL_OUTGOING(Direction.OUTGOING, false),
    ;

    MessageType(Direction direction, boolean failed) {
        this.direction = direction;
        this.failed = failed;
    }

    public enum Direction {
        INCOMING,
        OUTGOING,
    }

    private final Direction direction;
    private final boolean failed;

}
