package com.github.observant_sun.rattlegram.entity;

public record StatusUpdate(
        StatusType type,
        String message
) {
}
