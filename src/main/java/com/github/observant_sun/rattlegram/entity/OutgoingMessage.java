package com.github.observant_sun.rattlegram.entity;

public record OutgoingMessage(
        String callsign,
        String body,
        Integer delay
) {
}
