package com.project.mychat;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.mychat.domain.ConversationType;
import com.project.mychat.web.dto.AddParticipantRequest;
import com.project.mychat.web.dto.CreateConversationRequest;
import com.project.mychat.web.dto.EditMessageRequest;
import com.project.mychat.web.dto.SendMessageRequest;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises the REST layer end to end (real Postgres, real HTTP-shaped MockMvc requests) —
 * unlike {@link ChatMigrationIntegrationTest}, which stays at the service layer, this proves
 * the controllers, DTO (de)serialization, principal-based caller resolution, and the
 * ProblemDetail error mapping all work together.
 */
@Testcontainers
@SpringBootTest(classes = TestApplication.class, properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
class ChatApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private static Principal principal(String name) {
        return () -> name;
    }

    @Test
    void fullConversationLifecycleOverRest() throws Exception {
        String createBody = objectMapper.writeValueAsString(
                new CreateConversationRequest(ConversationType.GROUP, null, List.of("user-bob")));

        String conversationJson = mockMvc.perform(post("/api/chat/conversations")
                        .principal(principal("user-alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("GROUP"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String conversationId = objectMapper.readTree(conversationJson).get("id").asText();

        mockMvc.perform(get("/api/chat/conversations").principal(principal("user-alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        String sendBody = objectMapper.writeValueAsString(new SendMessageRequest("hello bob", null));
        String messageJson = mockMvc.perform(post("/api/chat/conversations/{id}/messages", conversationId)
                        .principal(principal("user-alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sendBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("hello bob"))
                .andExpect(jsonPath("$.messageType").value("TEXT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String messageId = objectMapper.readTree(messageJson).get("id").asText();

        mockMvc.perform(get("/api/chat/conversations/{id}/messages", conversationId).principal(principal("user-bob")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        String editBody = objectMapper.writeValueAsString(new EditMessageRequest("hello bob!"));
        mockMvc.perform(patch("/api/chat/messages/{id}", messageId)
                        .principal(principal("user-bob"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/chat/messages/{id}", messageId)
                        .principal(principal("user-alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("hello bob!"));

        mockMvc.perform(delete("/api/chat/messages/{id}", messageId).principal(principal("user-alice")))
                .andExpect(status().isNoContent());

        String addCarolBody = objectMapper.writeValueAsString(new AddParticipantRequest("user-carol"));
        mockMvc.perform(post("/api/chat/conversations/{id}/participants", conversationId)
                        .principal(principal("user-carol"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addCarolBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/chat/conversations/{id}/participants", conversationId)
                        .principal(principal("user-alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addCarolBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalUserId").value("user-carol"));

        mockMvc.perform(delete("/api/chat/conversations/{id}/participants/{user}", conversationId, "user-bob")
                        .principal(principal("user-bob")))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/chat/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isUnauthorized());
    }
}
