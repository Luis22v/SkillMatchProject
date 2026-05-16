package com.skillmatch.backend.service;

import com.skillmatch.backend.dto.ChatMessageResponse;
import com.skillmatch.backend.dto.MessageRequest;
import com.skillmatch.backend.exception.ResourceNotFoundException;
import com.skillmatch.backend.exception.UnauthorizedException;
import com.skillmatch.backend.model.Message;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public ChatMessageResponse sendMessage(@NonNull Long senderId, MessageRequest request) {
        if (senderId.equals(request.getReceiverId())) {
            throw new IllegalArgumentException("No puedes enviarte un mensaje a ti mismo");
        }

        User sender = userService.getUserById(senderId);

        Long receiverId = request.getReceiverId();
        if (receiverId == null) {
            throw new IllegalArgumentException("El ID del destinatario no puede ser nulo");
        }

        User receiver = userService.getUserById(receiverId);

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(request.getContent());
        message.setAttachmentUrl(request.getAttachmentUrl());
        message.setIsRead(false);
        message.setDeletedBySender(false);
        message.setDeletedByReceiver(false);

        message = messageRepository.save(message);

        notificationService.createMessageNotification(receiver.getId(), sender.getId(), message.getId());

        log.debug("Mensaje enviado: senderId={}, receiverId={}, messageId={}", senderId, receiverId, message.getId());
        return mapToResponse(message);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversation(Long userId, Long otherUserId) {
        List<Message> messages = messageRepository.findConversationBetweenUsers(userId, otherUserId);
        return messages.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageResponse markAsRead(Long messageId, Long userId) {
        if (messageId == null) {
            throw new IllegalArgumentException("El ID del mensaje no puede ser nulo");
        }
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado"));

        if (!message.getReceiver().getId().equals(userId)) {
            throw new UnauthorizedException("No tienes permiso para marcar este mensaje como leído");
        }

        if (!message.getIsRead()) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            message = messageRepository.save(message);
        }

        return mapToResponse(message);
    }

    @Transactional
    public void markConversationAsRead(Long userId, Long otherUserId) {
        List<Message> unreadMessages = messageRepository.findUnreadMessagesFromUser(userId, otherUserId);

        if (!unreadMessages.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (Message message : unreadMessages) {
                message.setIsRead(true);
                message.setReadAt(now);
            }
            messageRepository.saveAll(unreadMessages);
            log.debug("{} mensajes marcados como leídos en conversación userId={} con otherUserId={}", unreadMessages.size(), userId, otherUserId);
        }
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        if (messageId == null) {
            throw new IllegalArgumentException("El ID del mensaje no puede ser nulo");
        }
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado"));

        if (message.getSender().getId().equals(userId)) {
            message.setDeletedBySender(true);
        } else if (message.getReceiver().getId().equals(userId)) {
            message.setDeletedByReceiver(true);
        } else {
            throw new UnauthorizedException("No tienes permiso para eliminar este mensaje");
        }

        if (message.getDeletedBySender() && message.getDeletedByReceiver()) {
            messageRepository.delete(message);
        } else {
            messageRepository.save(message);
        }
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return messageRepository.countUnreadMessagesByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Long getUnreadCountFromUser(Long userId, Long fromUserId) {
        return messageRepository.countUnreadMessagesFromUser(userId, fromUserId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getLastMessages(Long userId) {
        List<Message> lastMessages = messageRepository.findLastMessagesByUserId(userId);
        return lastMessages.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getUnreadCountsByConversation(Long userId) {
        List<Message> unreadMessages = messageRepository.findUnreadMessagesByUserId(userId);

        return unreadMessages.stream()
                .collect(Collectors.groupingBy(
                        msg -> msg.getSender().getId(),
                        Collectors.counting()
                ));
    }

    private ChatMessageResponse mapToResponse(Message message) {
        User sender = message.getSender();
        User receiver = message.getReceiver();

        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setSenderId(sender.getId());
        response.setSenderName(sender.getFirstName() + " " + sender.getLastName());
        response.setSenderProfileImage(sender.getProfileImageUrl());
        response.setReceiverId(receiver.getId());
        response.setReceiverName(receiver.getFirstName() + " " + receiver.getLastName());
        response.setReceiverProfileImage(receiver.getProfileImageUrl());
        response.setContent(message.getContent());
        response.setIsRead(message.getIsRead());
        response.setReadAt(message.getReadAt());
        response.setSentAt(message.getSentAt());
        response.setAttachmentUrl(message.getAttachmentUrl());

        return response;
    }
}
