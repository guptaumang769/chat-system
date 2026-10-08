package com.umang.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umang.chat.dto.request.CreateConversationRequest;
import com.umang.chat.dto.request.SendMessageRequest;
import com.umang.chat.dto.response.ConversationResponse;
import com.umang.chat.dto.response.MessageResponse;
import com.umang.chat.event.MessageEvent;
import com.umang.chat.exception.InvalidRequestException;
import com.umang.chat.model.entity.Conversation;
import com.umang.chat.model.entity.Message;
import com.umang.chat.model.enums.ConversationType;
import com.umang.chat.model.enums.MessageStatus;
import com.umang.chat.outbox.OutboxService;
import com.umang.chat.repository.ConversationMemberRepository;
import com.umang.chat.repository.ConversationRepository;
import com.umang.chat.repository.MessageRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

/**
 * Pure unit tests for the chat send + status-machine + fan-out-target logic — no Docker.
 * All repositories and the outbox are mocked; we assert the service's contract: a send
 * persists a SENT message AND writes the outbox event in the same call, the status machine
 * only moves forward, and group fan-out targets every member except the sender.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationMemberRepository memberRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private OutboxService outboxService;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                conversationRepository, memberRepository, messageRepository, outboxService);
    }

    @Test
    void sendMessage_persistsAsSent_andWritesOutboxEvent() {
        SendMessageRequest request = SendMessageRequest.builder()
                .conversationId(1L).senderId(1L).content("hello").build();
        when(memberRepository.existsByConversationIdAndUserId(1L, 1L)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(99L); // simulate DB-assigned id
            return m;
        });

        MessageResponse response = chatService.sendMessage(request);

        // Persisted as SENT (single tick).
        ArgumentCaptor<Message> savedMessage = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(savedMessage.capture());
        assertThat(savedMessage.getValue().getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(savedMessage.getValue().getContent()).isEqualTo("hello");
        assertThat(response.getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(response.getId()).isEqualTo(99L);

        // Outbox event written in the same call, carrying the assigned message id.
        ArgumentCaptor<MessageEvent> event = ArgumentCaptor.forClass(MessageEvent.class);
        verify(outboxService).record(event.capture());
        assertThat(event.getValue().getMessageId()).isEqualTo(99L);
        assertThat(event.getValue().getConversationId()).isEqualTo(1L);
        assertThat(event.getValue().getStatus()).isEqualTo(MessageStatus.SENT);
    }

    @Test
    void sendMessage_rejectsNonMemberSender() {
        SendMessageRequest request = SendMessageRequest.builder()
                .conversationId(1L).senderId(7L).content("intruder").build();
        when(memberRepository.existsByConversationIdAndUserId(1L, 7L)).thenReturn(false);

        try {
            chatService.sendMessage(request);
            assertThat(false).as("expected InvalidRequestException").isTrue();
        } catch (RuntimeException expected) {
            // never persists nor writes outbox for a non-member
            verify(messageRepository, never()).save(any());
            verify(outboxService, never()).record(any());
        }
    }

    @Test
    void updateStatus_advancesSentToDeliveredToRead() {
        Message message = Message.builder()
                .id(5L).conversationId(1L).senderId(1L).content("hi")
                .status(MessageStatus.SENT).createdAt(Instant.now()).build();
        when(messageRepository.findById(5L)).thenReturn(Optional.of(message));

        Message afterDelivered = chatService.updateStatus(5L, MessageStatus.DELIVERED);
        assertThat(afterDelivered.getStatus()).isEqualTo(MessageStatus.DELIVERED);

        Message afterRead = chatService.updateStatus(5L, MessageStatus.READ);
        assertThat(afterRead.getStatus()).isEqualTo(MessageStatus.READ);

        verify(messageRepository, org.mockito.Mockito.times(2)).save(any(Message.class));
    }

    @Test
    void updateStatus_ignoresBackwardTransition() {
        Message message = Message.builder()
                .id(5L).conversationId(1L).senderId(1L).content("hi")
                .status(MessageStatus.READ).createdAt(Instant.now()).build();
        when(messageRepository.findById(5L)).thenReturn(Optional.of(message));

        // READ -> DELIVERED is backward; must be a no-op.
        Message result = chatService.updateStatus(5L, MessageStatus.DELIVERED);

        assertThat(result.getStatus()).isEqualTo(MessageStatus.READ);
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void recipientsOf_group_targetsAllMembersExceptSender() {
        // Group conversation with three members; sender is user 1.
        when(memberRepository.findMemberUserIds(2L)).thenReturn(List.of(1L, 2L, 3L));

        List<Long> recipients = chatService.recipientsOf(2L, 1L);

        assertThat(recipients).containsExactlyInAnyOrder(2L, 3L);
        assertThat(recipients).doesNotContain(1L);
    }

    @Test
    void recipientsOf_oneToOne_targetsTheOtherMember() {
        when(memberRepository.findMemberUserIds(1L)).thenReturn(List.of(1L, 2L));

        List<Long> recipients = chatService.recipientsOf(1L, 1L);

        assertThat(recipients).containsExactly(2L);
    }

    // --- createConversation ---

    @Test
    void createConversation_oneToOne_savesConversationAndMembers() {
        CreateConversationRequest request = CreateConversationRequest.builder()
                .type(ConversationType.ONE_TO_ONE)
                .memberIds(List.of(1L, 2L))
                .build();
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(50L);
            return c;
        });

        ConversationResponse response = chatService.createConversation(request);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getType()).isEqualTo(ConversationType.ONE_TO_ONE);
        assertThat(response.getMemberIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(response.getName()).isNull();
    }

    @Test
    void createConversation_oneToOne_rejectsWrongMemberCount() {
        CreateConversationRequest request = CreateConversationRequest.builder()
                .type(ConversationType.ONE_TO_ONE)
                .memberIds(List.of(1L, 2L, 3L))
                .build();

        assertThatThrownBy(() -> chatService.createConversation(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("exactly 2 members");
    }

    @Test
    void createConversation_rejectsFewerThanTwoMembers() {
        CreateConversationRequest request = CreateConversationRequest.builder()
                .type(ConversationType.GROUP)
                .memberIds(List.of(1L))
                .build();

        assertThatThrownBy(() -> chatService.createConversation(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("at least 2 members");
    }

    @Test
    void createConversation_deduplicatesMemberIds() {
        CreateConversationRequest request = CreateConversationRequest.builder()
                .type(ConversationType.ONE_TO_ONE)
                .memberIds(List.of(1L, 1L, 2L))
                .build();
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(51L);
            return c;
        });

        ConversationResponse response = chatService.createConversation(request);

        assertThat(response.getMemberIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    // --- getHistory ---

    @Test
    void getHistory_returnsMessagesInOrder() {
        Message m1 = Message.builder().id(1L).conversationId(10L).senderId(1L)
                .content("a").status(MessageStatus.SENT).createdAt(Instant.now()).build();
        Message m2 = Message.builder().id(2L).conversationId(10L).senderId(2L)
                .content("b").status(MessageStatus.SENT).createdAt(Instant.now()).build();
        when(messageRepository.findByConversationIdOrderByIdAsc(10L, PageRequest.of(0, 50)))
                .thenReturn(List.of(m1, m2));

        List<MessageResponse> history = chatService.getHistory(10L, null, null);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getId()).isEqualTo(1L);
        assertThat(history.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void getHistory_seekPagination_usesAfterCursor() {
        Message m3 = Message.builder().id(3L).conversationId(10L).senderId(1L)
                .content("c").status(MessageStatus.SENT).createdAt(Instant.now()).build();
        when(messageRepository.findByConversationIdAndIdGreaterThanOrderByIdAsc(
                10L, 2L, PageRequest.of(0, 50)))
                .thenReturn(List.of(m3));

        List<MessageResponse> history = chatService.getHistory(10L, 2L, null);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getId()).isEqualTo(3L);
    }

    @Test
    void getHistory_clampsLimitTo200() {
        when(messageRepository.findByConversationIdOrderByIdAsc(10L, PageRequest.of(0, 200)))
                .thenReturn(List.of());

        chatService.getHistory(10L, null, 999);

        verify(messageRepository).findByConversationIdOrderByIdAsc(10L, PageRequest.of(0, 200));
    }
}
