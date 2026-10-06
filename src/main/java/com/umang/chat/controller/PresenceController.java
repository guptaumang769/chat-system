package com.umang.chat.controller;

import com.umang.chat.dto.response.ApiResponse;
import com.umang.chat.dto.response.PresenceResponse;
import com.umang.chat.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<PresenceResponse>> getPresence(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(presenceService.getPresence(userId)));
    }
}
