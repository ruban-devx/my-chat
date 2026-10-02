package com.project.mychat.web.dto;

import com.project.mychat.domain.Participant;
import java.time.Instant;
import java.util.UUID;

public record ParticipantResponse(UUID id, String externalUserId, Instant joinedAt) {

    public static ParticipantResponse from(Participant participant) {
        return new ParticipantResponse(participant.getId(), participant.getExternalUserId(), participant.getJoinedAt());
    }
}
