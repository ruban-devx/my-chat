package com.project.mychat;

import com.project.mychat.domain.Conversation;
import com.project.mychat.domain.ConversationType;
import com.project.mychat.domain.Message;
import com.project.mychat.domain.MessageType;
import com.project.mychat.domain.Participant;
import com.project.mychat.repository.ParticipantRepository;
import com.project.mychat.service.ConversationService;
import com.project.mychat.service.MessageService;
import com.project.mychat.service.exception.NotAParticipantException;
import com.project.mychat.service.exception.ParticipantAlreadyActiveException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end check that the Flyway-migrated schema (db/migration/mychat) actually matches
 * the JPA entities (ddl-auto=validate) and that the service layer works against a real
 * Postgres instance, not just against a compiler.
 */
@Testcontainers
@SpringBootTest(classes = TestApplication.class, properties = "spring.jpa.hibernate.ddl-auto=validate")
class ChatMigrationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    ConversationService conversationService;

    @Autowired
    MessageService messageService;

    @Autowired
    ParticipantRepository participantRepository;

    @Test
    void migratedSchemaSupportsFullConversationLifecycle() {
        String alice = "user-alice";
        String bob = "user-bob";

        Conversation conversation = conversationService.createConversation(
                ConversationType.GROUP, null, List.of(alice, bob));
        assertThat(conversation.getId()).isNotNull();

        Message sent = messageService.sendMessage(conversation.getId(), alice, "hello bob", MessageType.TEXT);
        assertThat(sent.getId()).isNotNull();

        Page<Message> page = messageService.getMessages(conversation.getId(), bob, PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(Message::getContent).containsExactly("hello bob");

        Message edited = messageService.editMessage(sent.getId(), alice, "hello bob!");
        assertThat(edited.getContent()).isEqualTo("hello bob!");

        messageService.deleteMessage(sent.getId(), alice);
        Page<Message> afterDelete = messageService.getMessages(conversation.getId(), bob, PageRequest.of(0, 10));
        assertThat(afterDelete.getContent()).isEmpty();

        conversationService.removeParticipant(conversation.getId(), bob);
        assertThatThrownBy(() -> messageService.getMessages(conversation.getId(), bob, PageRequest.of(0, 10)))
                .isInstanceOf(NotAParticipantException.class);

        assertThat(conversationService.getConversations(alice))
                .extracting(Conversation::getId)
                .contains(conversation.getId());
    }

    @Test
    void addParticipantRejectsDuplicateActiveMembershipAtServiceLevel() {
        Conversation conversation = conversationService.createConversation(
                ConversationType.DIRECT, null, List.of("user-carol"));

        assertThatThrownBy(() -> conversationService.addParticipant(conversation.getId(), "user-carol"))
                .isInstanceOf(ParticipantAlreadyActiveException.class);
    }

    @Test
    void partialUniqueIndexRejectsDuplicateActiveMembershipAtDatabaseLevel() {
        Conversation conversation = conversationService.createConversation(
                ConversationType.DIRECT, null, List.of());

        Participant first = new Participant();
        first.setConversationId(conversation.getId());
        first.setExternalUserId("user-dave");
        participantRepository.saveAndFlush(first);

        Participant duplicate = new Participant();
        duplicate.setConversationId(conversation.getId());
        duplicate.setExternalUserId("user-dave");

        assertThatThrownBy(() -> participantRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
