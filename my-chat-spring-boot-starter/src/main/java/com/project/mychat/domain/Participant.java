package com.project.mychat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * A user's membership in a {@link Conversation}. {@code externalUserId} is the trusted
 * identifier chat receives from the host product's authenticated principal — chat does not
 * mint or manage user identities itself. Leaving a conversation sets {@code leftAt} rather
 * than deleting the row, so membership history survives removal.
 */
@Entity
@Table(name = "conversation_participants")
@Getter
@Setter
public class Participant {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "external_user_id", nullable = false, length = 100)
    private String externalUserId;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    private Instant leftAt;

    @PrePersist
    void onCreate() {
        joinedAt = Instant.now();
    }
}
