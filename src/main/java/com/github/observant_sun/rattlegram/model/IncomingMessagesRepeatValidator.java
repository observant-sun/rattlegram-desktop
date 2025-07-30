package com.github.observant_sun.rattlegram.model;

import com.github.observant_sun.rattlegram.entity.Message;
import com.github.observant_sun.rattlegram.entity.MessageType;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.*;

@Slf4j
public class IncomingMessagesRepeatValidator {

    private record CallsignBodyPair(String callsign, String body) {
    }

    private final Multimap<CallsignBodyPair, Message> keyToMessageMap = MultimapBuilder.hashKeys().hashSetValues().build();
    private final ObservableList<Message> incomingMessages;
    private final TemporalAmount debounceDuration;


    public IncomingMessagesRepeatValidator(ObservableList<Message> incomingMessages, TemporalAmount debounceDuration) {
        this.incomingMessages = incomingMessages;
        this.debounceDuration = debounceDuration;
        populateDebounceCandidates();
    }

    private void populateDebounceCandidates() {
        keyToMessageMap.clear();
        incomingMessages.stream()
                .filter(Objects::nonNull)
                .filter(message -> message.type().getDirection() == MessageType.Direction.INCOMING)
                .filter(message -> message.timestamp().isAfter(LocalDateTime.now().minus(debounceDuration)))
                .forEach(message -> keyToMessageMap.put(new CallsignBodyPair(message.callsign(), message.body()), message));
    }

    public boolean isValidForRepeat(Message message) {
        if (message.type().getDirection() == MessageType.Direction.OUTGOING) {
            log.debug("message {} direction is OUTGOING, invalid", message.type());
            return false;
        }
        if (message.type().isFailed()) {
            log.debug("message {} is failed, invalid", message.type());
            return false;
        }
        if (message.callsign() == null || message.callsign().isEmpty()) {
            log.debug("message {} callsign is empty, invalid", message.type());
            return false;
        }
        CallsignBodyPair key = new CallsignBodyPair(message.callsign(), message.body());
        log.debug("key = {}", key);
        Collection<Message> messages = keyToMessageMap.get(key);
        log.debug("messages = {}", messages);
        messages.removeIf(m -> message.timestamp().isBefore(LocalDateTime.now().minus(debounceDuration)));
        boolean valid = messages.isEmpty();
        keyToMessageMap.put(key, message);
        return valid;
    }


}
