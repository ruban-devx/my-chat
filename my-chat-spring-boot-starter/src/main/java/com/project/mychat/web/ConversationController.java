package com.project.mychat.web;

import com.project.mychat.domain.Conversation;
import com.project.mychat.domain.ExternalReference;
import com.project.mychat.domain.Participant;
import com.project.mychat.service.ConversationService;
import com.project.mychat.web.dto.AddParticipantRequest;
import com.project.mychat.web.dto.ConversationResponse;
import com.project.mychat.web.dto.CreateConversationRequest;
import com.project.mychat.web.dto.ExternalReferenceDto;
import com.project.mychat.web.dto.ParticipantResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/chat/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ConversationResponse createConversation(@Valid @RequestBody CreateConversationRequest request, Principal principal) {
        String callerId = CallerId.require(principal);

        List<String> participants = new ArrayList<>();
        if (request.participantExternalUserIds() != null) {
            participants.addAll(request.participantExternalUserIds());
        }
        if (!participants.contains(callerId)) {
            participants.add(callerId);
        }

        Conversation conversation = conversationService.createConversation(
                request.type(), toExternalReference(request.externalReference()), participants);
        return ConversationResponse.from(conversation);
    }

    @GetMapping
    public List<ConversationResponse> getConversations(Principal principal) {
        String callerId = CallerId.require(principal);
        return conversationService.getConversations(callerId).stream()
                .map(ConversationResponse::from)
                .toList();
    }

    @PostMapping("/{conversationId}/participants")
    public ParticipantResponse addParticipant(
            @PathVariable UUID conversationId, @Valid @RequestBody AddParticipantRequest request, Principal principal) {
        String callerId = CallerId.require(principal);
        requireActiveMember(conversationId, callerId);

        Participant participant = conversationService.addParticipant(conversationId, request.externalUserId());
        return ParticipantResponse.from(participant);
    }

    @DeleteMapping("/{conversationId}/participants/{externalUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeParticipant(@PathVariable UUID conversationId, @PathVariable String externalUserId, Principal principal) {
        String callerId = CallerId.require(principal);
        if (!callerId.equals(externalUserId)) {
            requireActiveMember(conversationId, callerId);
        }
        conversationService.removeParticipant(conversationId, externalUserId);
    }

    private void requireActiveMember(UUID conversationId, String callerId) {
        if (!conversationService.isActiveParticipant(conversationId, callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Caller is not a participant of this conversation");
        }
    }

    private ExternalReference toExternalReference(ExternalReferenceDto dto) {
        if (dto == null) {
            return null;
        }
        ExternalReference reference = new ExternalReference();
        reference.setType(dto.type());
        reference.setId(dto.id());
        return reference;
    }
}
