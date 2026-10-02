package com.project.mychat.web;

import com.project.mychat.MyChatAutoConfiguration;
import com.project.mychat.service.exception.ChatException;
import com.project.mychat.service.exception.ConversationNotFoundException;
import com.project.mychat.service.exception.ForbiddenMessageActionException;
import com.project.mychat.service.exception.MessageNotFoundException;
import com.project.mychat.service.exception.NotAParticipantException;
import com.project.mychat.service.exception.ParticipantAlreadyActiveException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Scoped to this starter's own controllers (via {@code basePackageClasses}) so it never
 * intercepts exceptions from the host application's unrelated endpoints.
 */
@RestControllerAdvice(basePackageClasses = MyChatAutoConfiguration.class)
public class ChatExceptionHandler {

    @ExceptionHandler({ConversationNotFoundException.class, MessageNotFoundException.class})
    ProblemDetail handleNotFound(ChatException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({NotAParticipantException.class, ForbiddenMessageActionException.class})
    ProblemDetail handleForbidden(ChatException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ParticipantAlreadyActiveException.class)
    ProblemDetail handleConflict(ChatException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
