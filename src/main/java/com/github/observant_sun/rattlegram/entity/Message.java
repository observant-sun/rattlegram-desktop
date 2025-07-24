package com.github.observant_sun.rattlegram.entity;

import java.time.LocalDateTime;

public record Message(
        String callsign,
        String body,
        LocalDateTime timestamp,
        MessageType type
) {
}
