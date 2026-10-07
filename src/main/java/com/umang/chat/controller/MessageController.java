package com.umang.chat.controller;

import com.umang.chat.dto.request.SendMessageRequest;
import com.umang.chat.dto.response.ApiResponse;
import com.umang.chat.dto.response.MessageResponse;
import com.umang.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST send fallback for clients that can't hold a WebSocket. */
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> send(
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chatService.sendMessage(request)));
    }
}
