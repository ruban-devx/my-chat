package com.project.mychat.service;

import com.project.mychat.domain.Conversation;
import com.project.mychat.domain.ConversationType;
import com.project.mychat.domain.ExternalReference;
import com.project.mychat.domain.Participant;
import com.project.mychat.repository.ConversationRepository;
import com.project.mychat.repository.ParticipantRepository;
import com.project.mychat.service.exception.ConversationNotFoundException;
import com.project.mychat.service.exception.NotAParticipantException;
import com.project.mychat.service.exception.ParticipantAlreadyActiveException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Creates conversations and manages who is an active participant in them. */
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ParticipantRepository participantRepository;

    public ConversationService(ConversationRepository conversationRepository, ParticipantRepository participantRepository) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
    }

    @Transactional
    public Conversation createConversation(
            ConversationType type, ExternalReference externalReference, List<String> initialParticipantExternalUserIds) {
        Conversation conversation = new Conversation();
        conversation.setType(type);
        conversation.setExternalReference(externalReference);
        conversation = conversationRepository.save(conversation);

        for (String externalUserId : initialParticipantExternalUserIds) {
            addParticipant(conversation.getId(), externalUserId);
        }
        return conversation;
    }

    @Transactional
    public Participant addParticipant(UUID conversationId, String externalUserId) {
        requireConversation(conversationId);
        participantRepository
                .findByConversationIdAndExternalUserIdAndLeftAtIsNull(conversationId, externalUserId)
                .ifPresent(p -> {
                    throw new ParticipantAlreadyActiveException(conversationId, externalUserId);
                });

        Participant participant = new Participant();
        participant.setConversationId(conversationId);
        participant.setExternalUserId(externalUserId);
        return participantRepository.save(participant);
    }

    @Transactional
    public void removeParticipant(UUID conversationId, String externalUserId) {
        Participant participant = participantRepository
                .findByConversationIdAndExternalUserIdAndLeftAtIsNull(conversationId, externalUserId)
                .orElseThrow(() -> new NotAParticipantException(conversationId, externalUserId));
        participant.setLeftAt(Instant.now());
        participantRepository.save(participant);
    }

    @Transactional(readOnly = true)
    public List<Conversation> getConversations(String externalUserId) {
        return conversationRepository.findActiveForParticipant(externalUserId);
    }

    @Transactional(readOnly = true)
    public boolean isActiveParticipant(UUID conversationId, String externalUserId) {
        return participantRepository
                .findByConversationIdAndExternalUserIdAndLeftAtIsNull(conversationId, externalUserId)
                .isPresent();
    }

    private Conversation requireConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }
}
