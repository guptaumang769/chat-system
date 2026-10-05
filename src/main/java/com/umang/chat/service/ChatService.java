package com.umang.chat.service;

import com.umang.chat.dto.request.CreateConversationRequest;
import com.umang.chat.dto.request.SendMessageRequest;
import com.umang.chat.dto.response.ConversationResponse;
import com.umang.chat.dto.response.MessageResponse;
import com.umang.chat.event.MessageEvent;
import com.umang.chat.exception.InvalidRequestException;
import com.umang.chat.exception.ResourceNotFoundException;
import com.umang.chat.model.entity.Conversation;
import com.umang.chat.model.entity.ConversationMember;
import com.umang.chat.model.entity.Message;
import com.umang.chat.model.enums.ConversationType;
import com.umang.chat.model.enums.MessageStatus;
import com.umang.chat.outbox.OutboxService;
import com.umang.chat.repository.ConversationMemberRepository;
import com.umang.chat.repository.ConversationRepository;
import com.umang.chat.repository.MessageRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Conversations, messages, delivery status, and paginated history. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final OutboxService outboxService;

    private static final int DEFAULT_PAGE_SIZE = 50;

    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {
        List<Long> memberIds = request.getMemberIds().stream().distinct().toList();
        if (request.getType() == ConversationType.ONE_TO_ONE && memberIds.size() != 2) {
            throw new InvalidRequestException("A ONE_TO_ONE conversation must have exactly 2 members");
        }
        if (memberIds.size() < 2) {
            throw new InvalidRequestException("A conversation needs at least 2 members");
        }

        Conversation conversation = conversationRepository.save(Conversation.builder()
                .type(request.getType())
                .name(request.getType() == ConversationType.GROUP ? request.getName() : null)
                .build());

        for (Long userId : memberIds) {
            memberRepository.save(ConversationMember.builder()
                    .conversationId(conversation.getId())
                    .userId(userId)
                    .build());
        }
        return ConversationResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .name(conversation.getName())
                .memberIds(memberIds)
                .build();
    }

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request) {
        if (!memberRepository.existsByConversationIdAndUserId(
                request.getConversationId(), request.getSenderId())) {
            throw new InvalidRequestException("Sender is not a member of this conversation");
        }

        Message message = messageRepository.save(Message.builder()
                .conversationId(request.getConversationId())
                .senderId(request.getSenderId())
                .content(request.getContent())
                .status(MessageStatus.SENT)
                .createdAt(Instant.now())
                .build());

        outboxService.record(MessageEvent.builder()
                .messageId(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .build());

        return MessageResponse.from(message);
    }

    public List<Long> recipientsOf(Long conversationId, Long senderId) {
        return memberRepository.findMemberUserIds(conversationId).stream()
                .filter(id -> !id.equals(senderId))
                .toList();
    }

    @Transactional
    public Message updateStatus(Long messageId, MessageStatus newStatus) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
        if (isForwardTransition(message.getStatus(), newStatus)) {
            message.setStatus(newStatus);
            messageRepository.save(message);
        }
        return message;
    }

    private boolean isForwardTransition(MessageStatus current, MessageStatus next) {
        return next.ordinal() > current.ordinal();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getHistory(Long conversationId, Long after, Integer limit) {
        int pageSize = (limit == null || limit <= 0) ? DEFAULT_PAGE_SIZE : Math.min(limit, 200);
        PageRequest page = PageRequest.of(0, pageSize);
        List<Message> messages = (after == null)
                ? messageRepository.findByConversationIdOrderByIdAsc(conversationId, page)
                : messageRepository.findByConversationIdAndIdGreaterThanOrderByIdAsc(
                        conversationId, after, page);
        return messages.stream().map(MessageResponse::from).toList();
    }
}
