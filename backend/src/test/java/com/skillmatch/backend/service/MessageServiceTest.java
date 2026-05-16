package com.skillmatch.backend.service;

import com.skillmatch.backend.dto.ChatMessageResponse;
import com.skillmatch.backend.dto.MessageRequest;
import com.skillmatch.backend.exception.ResourceNotFoundException;
import com.skillmatch.backend.exception.UnauthorizedException;
import com.skillmatch.backend.model.Message;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private UserService userService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private MessageService messageService;

    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setFirstName("Ana");
        sender.setLastName("García");
        sender.setEmail("ana@test.com");

        receiver = new User();
        receiver.setId(2L);
        receiver.setFirstName("Luis");
        receiver.setLastName("Martínez");
        receiver.setEmail("luis@test.com");
    }

    // ─── sendMessage ──────────────────────────────────────────────────────────

    @Test
    void sendMessage_happyPath_savesAndNotifies() {
        MessageRequest req = new MessageRequest(2L, "Hola!", null);
        when(userService.getUserById(1L)).thenReturn(sender);
        when(userService.getUserById(2L)).thenReturn(receiver);
        Message saved = buildMessage(10L, sender, receiver);
        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        ChatMessageResponse response = messageService.sendMessage(1L, req);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getIsRead()).isFalse();
        assertThat(captor.getValue().getDeletedBySender()).isFalse();
        assertThat(captor.getValue().getDeletedByReceiver()).isFalse();

        verify(notificationService).createMessageNotification(
                eq(receiver.getId()), eq(sender.getId()), eq(saved.getId()));

        assertThat(response.getSenderId()).isEqualTo(1L);
        assertThat(response.getReceiverId()).isEqualTo(2L);
    }

    @Test
    void sendMessage_toSelf_throwsIllegalArgument() {
        MessageRequest req = new MessageRequest(1L, "Hola!", null);

        assertThatThrownBy(() -> messageService.sendMessage(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ti mismo");
    }

    @Test
    void sendMessage_nullReceiverId_throwsIllegalArgument() {
        MessageRequest req = new MessageRequest(null, "Hola!", null);
        when(userService.getUserById(1L)).thenReturn(sender);

        assertThatThrownBy(() -> messageService.sendMessage(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulo");
    }

    @Test
    void sendMessage_senderNotFound_throwsResourceNotFound() {
        MessageRequest req = new MessageRequest(2L, "Hola!", null);
        when(userService.getUserById(1L)).thenThrow(new ResourceNotFoundException("Usuario no encontrado"));

        assertThatThrownBy(() -> messageService.sendMessage(1L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sendMessage_receiverNotFound_throwsResourceNotFound() {
        MessageRequest req = new MessageRequest(2L, "Hola!", null);
        when(userService.getUserById(1L)).thenReturn(sender);
        when(userService.getUserById(2L)).thenThrow(new ResourceNotFoundException("Usuario no encontrado"));

        assertThatThrownBy(() -> messageService.sendMessage(1L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sendMessage_noNotificationWhenSaveFails() {
        MessageRequest req = new MessageRequest(2L, "Hola!", null);
        when(userService.getUserById(1L)).thenReturn(sender);
        when(userService.getUserById(2L)).thenReturn(receiver);
        when(messageRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> messageService.sendMessage(1L, req))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(notificationService);
    }

    // ─── getConversation ──────────────────────────────────────────────────────

    @Test
    void getConversation_returnsMappedList() {
        Message m1 = buildMessage(11L, sender, receiver);
        Message m2 = buildMessage(12L, receiver, sender);
        when(messageRepository.findConversationBetweenUsers(1L, 2L)).thenReturn(List.of(m1, m2));

        List<ChatMessageResponse> result = messageService.getConversation(1L, 2L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(11L);
        assertThat(result.get(1).getId()).isEqualTo(12L);
    }

    @Test
    void getConversation_empty_returnsEmptyList() {
        when(messageRepository.findConversationBetweenUsers(1L, 2L)).thenReturn(List.of());

        assertThat(messageService.getConversation(1L, 2L)).isEmpty();
    }

    // ─── markAsRead ───────────────────────────────────────────────────────────

    @Test
    void markAsRead_happyPath_setsReadAndSaves() {
        Message msg = buildMessage(10L, sender, receiver);
        msg.setIsRead(false);
        when(messageRepository.findById(10L)).thenReturn(Optional.of(msg));
        when(messageRepository.save(any(Message.class))).thenReturn(msg);

        messageService.markAsRead(10L, receiver.getId());

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getIsRead()).isTrue();
        assertThat(captor.getValue().getReadAt()).isNotNull();
    }

    @Test
    void markAsRead_alreadyRead_skipsUpdate() {
        Message msg = buildMessage(10L, sender, receiver);
        msg.setIsRead(true);
        when(messageRepository.findById(10L)).thenReturn(Optional.of(msg));

        messageService.markAsRead(10L, receiver.getId());

        verify(messageRepository, never()).save(any());
    }

    @Test
    void markAsRead_notFound_throwsResourceNotFound() {
        when(messageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.markAsRead(99L, receiver.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAsRead_wrongUser_throwsUnauthorized() {
        Message msg = buildMessage(10L, sender, receiver);
        when(messageRepository.findById(10L)).thenReturn(Optional.of(msg));

        assertThatThrownBy(() -> messageService.markAsRead(10L, sender.getId()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void markAsRead_nullMessageId_throwsIllegalArgument() {
        assertThatThrownBy(() -> messageService.markAsRead(null, receiver.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── markConversationAsRead ───────────────────────────────────────────────

    @Test
    void markConversationAsRead_happyPath_setsIsReadAndSavesAll() {
        Message m1 = buildMessage(11L, sender, receiver);
        Message m2 = buildMessage(12L, sender, receiver);
        when(messageRepository.findUnreadMessagesFromUser(2L, 1L)).thenReturn(List.of(m1, m2));

        messageService.markConversationAsRead(2L, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(messageRepository).saveAll(captor.capture());

        List<Message> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(m -> m.getIsRead());
        assertThat(saved).allMatch(m -> m.getReadAt() != null);
    }

    @Test
    void markConversationAsRead_emptyList_noSaveAll() {
        when(messageRepository.findUnreadMessagesFromUser(2L, 1L)).thenReturn(List.of());

        messageService.markConversationAsRead(2L, 1L);

        verify(messageRepository, never()).saveAll(any());
    }

    // ─── deleteMessage ────────────────────────────────────────────────────────

    @Test
    void deleteMessage_bySender_setsSenderFlagAndSaves() {
        Message msg = buildMessage(10L, sender, receiver);
        when(messageRepository.findById(10L)).thenReturn(Optional.of(msg));

        messageService.deleteMessage(10L, sender.getId());

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedBySender()).isTrue();
        verify(messageRepository, never()).delete(any());
    }

    @Test
    void deleteMessage_byReceiver_setsReceiverFlagAndSaves() {
        Message msg = buildMessage(10L, sender, receiver);
        when(messageRepository.findById(10L)).thenReturn(Optional.of(msg));

        messageService.deleteMessage(10L, receiver.getId());

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedByReceiver()).isTrue();
        verify(messageRepository, never()).delete(any());
    }

    @Test
    void deleteMessage_bothDeleted_callsHardDelete() {
        Message msg = buildMessage(10L, sender, receiver);
        msg.setDeletedBySender(true);               // sender already deleted
        when(messageRepository.findById(10L)).thenReturn(Optional.of(msg));

        messageService.deleteMessage(10L, receiver.getId());   // receiver deletes → both true

        verify(messageRepository).delete(msg);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void deleteMessage_thirdParty_throwsUnauthorized() {
        Message msg = buildMessage(10L, sender, receiver);
        when(messageRepository.findById(10L)).thenReturn(Optional.of(msg));

        assertThatThrownBy(() -> messageService.deleteMessage(10L, 99L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deleteMessage_notFound_throwsResourceNotFound() {
        when(messageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.deleteMessage(99L, sender.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteMessage_nullMessageId_throwsIllegalArgument() {
        assertThatThrownBy(() -> messageService.deleteMessage(null, sender.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── getUnreadCount ───────────────────────────────────────────────────────

    @Test
    void getUnreadCount_returnsCount() {
        when(messageRepository.countUnreadMessagesByUserId(2L)).thenReturn(5L);

        assertThat(messageService.getUnreadCount(2L)).isEqualTo(5L);
    }

    // ─── getUnreadCountFromUser ───────────────────────────────────────────────

    @Test
    void getUnreadCountFromUser_returnsCount() {
        when(messageRepository.countUnreadMessagesFromUser(2L, 1L)).thenReturn(3L);

        assertThat(messageService.getUnreadCountFromUser(2L, 1L)).isEqualTo(3L);
    }

    // ─── getLastMessages ──────────────────────────────────────────────────────

    @Test
    void getLastMessages_returnsMappedList() {
        Message m1 = buildMessage(11L, sender, receiver);
        Message m2 = buildMessage(12L, receiver, sender);
        when(messageRepository.findLastMessagesByUserId(1L)).thenReturn(List.of(m1, m2));

        List<ChatMessageResponse> result = messageService.getLastMessages(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(11L);
    }

    @Test
    void getLastMessages_empty_returnsEmptyList() {
        when(messageRepository.findLastMessagesByUserId(1L)).thenReturn(List.of());

        assertThat(messageService.getLastMessages(1L)).isEmpty();
    }

    // ─── getUnreadCountsByConversation ────────────────────────────────────────

    @Test
    void getUnreadCountsByConversation_groupsBySenderId() {
        User senderB = new User();
        senderB.setId(3L);
        senderB.setFirstName("Carlos");
        senderB.setLastName("López");
        senderB.setEmail("carlos@test.com");

        Message m1 = buildMessage(11L, sender,  receiver);  // sender id=1
        Message m2 = buildMessage(12L, sender,  receiver);  // sender id=1
        Message m3 = buildMessage(13L, senderB, receiver);  // sender id=3

        when(messageRepository.findUnreadMessagesByUserId(receiver.getId()))
                .thenReturn(List.of(m1, m2, m3));

        Map<Long, Long> result = messageService.getUnreadCountsByConversation(receiver.getId());

        assertThat(result).containsEntry(1L, 2L)
                          .containsEntry(3L, 1L)
                          .hasSize(2);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Message buildMessage(Long id, User msgSender, User msgReceiver) {
        Message msg = new Message();
        msg.setId(id);
        msg.setSender(msgSender);
        msg.setReceiver(msgReceiver);
        msg.setContent("Mensaje de prueba");
        msg.setIsRead(false);
        msg.setDeletedBySender(false);
        msg.setDeletedByReceiver(false);
        return msg;
    }
}
