package com.cleanfresh.ms_cleanfresh_bff.controller;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health(JwtAuthenticationToken authentication) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        response.put("user", buildUserInfo(authentication));
        return response;
    }

    private Map<String, Object> buildUserInfo(JwtAuthenticationToken authentication) {
        if (authentication == null) {
            return null;
        }

        Jwt jwt = authentication.getToken();
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("subject", jwt.getSubject());
        user.put("name", jwt.getClaimAsString("name"));
        user.put("preferredUsername", jwt.getClaimAsString("preferred_username"));
        user.put("authorities", authentication.getAuthorities().stream()
                .map(Object::toString)
                .toList());
        return user;
    }
}
