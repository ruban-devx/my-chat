package com.example.sample.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** See {@link DemoAuthFilter} for why this exists instead of real authentication. */
@RestController
public class DemoAuthController {

    @PostMapping("/demo/login")
    public void login(@RequestParam String user, HttpServletResponse response) {
        Cookie cookie = new Cookie(DemoAuthFilter.COOKIE_NAME, user);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    @PostMapping("/demo/logout")
    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie(DemoAuthFilter.COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @GetMapping("/demo/me")
    public Map<String, String> me(Principal principal) {
        return Map.of("userId", principal == null ? "" : principal.getName());
    }
}
