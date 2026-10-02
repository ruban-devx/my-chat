package com.example.sample.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Stand-in for real authentication, existing only to prove the point made throughout
 * my-chat-spring-boot-starter's design: chat consumes whatever {@link Principal} the host app's
 * request already carries, however that got there. This is deliberately NOT Spring Security —
 * a plain cookie naming "who's logged in" is enough to prove the mechanism-agnostic identity
 * boundary actually works, without a real product needing to adopt any particular auth stack.
 *
 * <p>Cookie-based (not a header) specifically because a browser attaches cookies automatically
 * to both REST {@code fetch} calls <em>and</em> the WebSocket/SockJS handshake — a header-based
 * scheme (e.g. HTTP Basic) can't reach the WebSocket upgrade request at all, since browsers
 * don't allow arbitrary headers on it.
 */
@Component
public class DemoAuthFilter extends OncePerRequestFilter {

    static final String COOKIE_NAME = "demo-user";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String userId = readCookie(request);
        HttpServletRequest effectiveRequest = userId == null ? request : withPrincipal(request, userId);
        chain.doFilter(effectiveRequest, response);
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private HttpServletRequest withPrincipal(HttpServletRequest request, String userId) {
        Principal principal = () -> userId;
        return new HttpServletRequestWrapper(request) {
            @Override
            public Principal getUserPrincipal() {
                return principal;
            }
        };
    }
}
