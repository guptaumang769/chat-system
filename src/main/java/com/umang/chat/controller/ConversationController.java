package com.umang.chat.controller;

import com.umang.chat.dto.request.CreateConversationRequest;
import com.umang.chat.dto.response.ApiResponse;
import com.umang.chat.dto.response.ConversationResponse;
import com.umang.chat.dto.response.MessageResponse;
import com.umang.chat.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> create(
            @Valid @RequestBody CreateConversationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chatService.createConversation(request)));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> history(
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.success(
                chatService.getHistory(conversationId, after, limit)));
    }
}
