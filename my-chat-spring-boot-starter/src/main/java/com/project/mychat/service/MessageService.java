package com.project.mychat.service;

import com.project.mychat.domain.Message;
import com.project.mychat.domain.MessageType;
import com.project.mychat.event.MessageEvent;
import com.project.mychat.repository.MessageRepository;
import com.project.mychat.service.exception.ForbiddenMessageActionException;
import com.project.mychat.service.exception.MessageNotFoundException;
import com.project.mychat.service.exception.NotAParticipantException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/** Sends, edits, deletes, and lists messages within a conversation. */
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final ApplicationEventPublisher eventPublisher;

    public MessageService(
            MessageRepository messageRepository, ConversationService conversationService, ApplicationEventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Message sendMessage(UUID conversationId, String senderExternalUserId, String content, MessageType type) {
        requireActiveParticipant(conversationId, senderExternalUserId);

        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderExternalUserId(senderExternalUserId);
        message.setContent(content);
        message.setMessageType(type);
        Message saved = messageRepository.save(message);
        eventPublisher.publishEvent(new MessageEvent(MessageEvent.Type.CREATED, saved));
        return saved;
    }

    @Transactional
    public Message editMessage(UUID messageId, String editorExternalUserId, String newContent) {
        Message message = requireMessage(messageId);
        requireSender(message, editorExternalUserId);

        message.setContent(newContent);
        Message saved = messageRepository.save(message);
        eventPublisher.publishEvent(new MessageEvent(MessageEvent.Type.UPDATED, saved));
        return saved;
    }

    @Transactional
    public void deleteMessage(UUID messageId, String requesterExternalUserId) {
        Message message = requireMessage(messageId);
        requireSender(message, requesterExternalUserId);

        message.setDeletedAt(Instant.now());
        Message saved = messageRepository.save(message);
        eventPublisher.publishEvent(new MessageEvent(MessageEvent.Type.DELETED, saved));
    }

    @Transactional(readOnly = true)
    public Page<Message> getMessages(UUID conversationId, String requesterExternalUserId, Pageable pageable) {
        requireActiveParticipant(conversationId, requesterExternalUserId);
        return messageRepository.findByConversationIdAndDeletedAtIsNullOrderByCreatedAtDesc(conversationId, pageable);
    }

    private Message requireMessage(UUID messageId) {
        return messageRepository.findByIdAndDeletedAtIsNull(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
    }

    private void requireSender(Message message, String externalUserId) {
        if (!message.getSenderExternalUserId().equals(externalUserId)) {
            throw new ForbiddenMessageActionException(message.getId(), externalUserId);
        }
    }

    private void requireActiveParticipant(UUID conversationId, String externalUserId) {
        if (!conversationService.isActiveParticipant(conversationId, externalUserId)) {
            throw new NotAParticipantException(conversationId, externalUserId);
        }
    }
}
