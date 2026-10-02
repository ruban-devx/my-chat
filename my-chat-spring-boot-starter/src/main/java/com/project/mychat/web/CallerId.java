package com.project.mychat.web;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolves the trusted caller identity from the servlet request's {@link Principal} —
 * deliberately the plain JDK/servlet type, not a Spring Security type, so this starter never
 * requires a specific authentication mechanism from the host app.
 */
final class CallerId {

    private CallerId() {
    }

    static String require(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated principal on this request");
        }
        return principal.getName();
    }
}
