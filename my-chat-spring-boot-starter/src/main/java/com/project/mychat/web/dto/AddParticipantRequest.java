package com.project.mychat.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AddParticipantRequest(@NotBlank String externalUserId) {
}
