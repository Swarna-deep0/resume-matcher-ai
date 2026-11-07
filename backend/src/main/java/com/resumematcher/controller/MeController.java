package com.resumematcher.controller;

import com.resumematcher.dto.UserResponse;
import com.resumematcher.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        // The user id comes only from the validated token, never from the request
        Long userId = Long.valueOf(jwt.getSubject());
        return UserResponse.from(userService.getById(userId));
    }
}