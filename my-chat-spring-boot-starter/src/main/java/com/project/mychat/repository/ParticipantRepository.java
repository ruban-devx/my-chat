package com.project.mychat.repository;

import com.project.mychat.domain.Participant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    List<Participant> findByConversationIdAndLeftAtIsNull(UUID conversationId);

    Optional<Participant> findByConversationIdAndExternalUserIdAndLeftAtIsNull(
            UUID conversationId, String externalUserId);
}
