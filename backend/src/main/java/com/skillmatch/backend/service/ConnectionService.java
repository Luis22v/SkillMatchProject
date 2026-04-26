package com.skillmatch.backend.service;

import com.skillmatch.backend.dto.ConnectionRequest;
import com.skillmatch.backend.dto.ConnectionResponse;
import com.skillmatch.backend.exception.DuplicateResourceException;
import com.skillmatch.backend.exception.ResourceNotFoundException;
import com.skillmatch.backend.exception.UnauthorizedException;
import com.skillmatch.backend.model.Connection;
import com.skillmatch.backend.model.ConnectionStatus;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.repository.ConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public ConnectionResponse sendConnectionRequest(Long userId, ConnectionRequest request) {
        if (userId.equals(request.getConnectedUserId())) {
            throw new IllegalArgumentException("No puedes enviarte una solicitud a ti mismo");
        }

        User user = userService.getUserById(userId);

        Long connectedUserId = request.getConnectedUserId();
        if (connectedUserId == null) {
            throw new IllegalArgumentException("ID de usuario conectado no puede ser nulo");
        }

        User connectedUser = userService.getUserById(connectedUserId);

        if (connectionRepository.findConnectionBetweenUsers(userId, request.getConnectedUserId()).isPresent()) {
            throw new DuplicateResourceException("Ya existe una conexión o solicitud pendiente con este usuario");
        }

        Connection connection = new Connection();
        connection.setUser(user);
        connection.setConnectedUser(connectedUser);
        connection.setStatus(ConnectionStatus.PENDING);
        connection.setMessage(request.getMessage());
        connection.setRequestedAt(LocalDateTime.now());

        connection = connectionRepository.save(connection);

        notificationService.createConnectionRequestNotification(connectedUser.getId(), user.getId(), connection.getId());

        log.info("Solicitud de conexión enviada: userId={} → connectedUserId={}", userId, connectedUserId);
        return mapToResponse(connection);
    }

    @Transactional
    public ConnectionResponse acceptConnection(Long connectionId, Long userId) {
        if (connectionId == null) {
            throw new IllegalArgumentException("ID de conexión no puede ser nulo");
        }
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de conexión no encontrada"));

        if (!connection.getConnectedUser().getId().equals(userId)) {
            throw new UnauthorizedException("No tienes permiso para aceptar esta solicitud");
        }

        if (ConnectionStatus.PENDING != connection.getStatus()) {
            throw new IllegalArgumentException("Esta solicitud ya fue procesada");
        }

        connection.setStatus(ConnectionStatus.ACCEPTED);
        connection.setRespondedAt(LocalDateTime.now());
        connection = connectionRepository.save(connection);

        notificationService.createConnectionAcceptedNotification(connection.getUser().getId(), userId, connectionId);

        log.info("Conexión {} aceptada por usuario {}", connectionId, userId);
        return mapToResponse(connection);
    }

    @Transactional
    public ConnectionResponse rejectConnection(Long connectionId, Long userId) {
        if (connectionId == null) {
            throw new IllegalArgumentException("ID de conexión no puede ser nulo");
        }
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de conexión no encontrada"));

        if (!connection.getConnectedUser().getId().equals(userId)) {
            throw new UnauthorizedException("No tienes permiso para rechazar esta solicitud");
        }

        if (ConnectionStatus.PENDING != connection.getStatus()) {
            throw new IllegalArgumentException("Esta solicitud ya fue procesada");
        }

        connection.setStatus(ConnectionStatus.REJECTED);
        connection.setRespondedAt(LocalDateTime.now());
        connection = connectionRepository.save(connection);

        log.info("Conexión {} rechazada por usuario {}", connectionId, userId);
        return mapToResponse(connection);
    }

    @Transactional
    public void blockConnection(Long connectionId, Long userId) {
        if (connectionId == null) {
            throw new IllegalArgumentException("ID de conexión no puede ser nulo");
        }
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Conexión no encontrada"));

        if (!connection.getUser().getId().equals(userId) && !connection.getConnectedUser().getId().equals(userId)) {
            throw new UnauthorizedException("No tienes permiso para bloquear esta conexión");
        }

        connection.setStatus(ConnectionStatus.BLOCKED);
        connectionRepository.save(connection);
        log.warn("Conexión {} bloqueada por usuario {}", connectionId, userId);
    }

    @Transactional(readOnly = true)
    public List<ConnectionResponse> getMyConnections(Long userId) {
        List<Connection> connections = connectionRepository.findAcceptedConnectionsByUserId(userId);
        return connections.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConnectionResponse> getPendingRequests(Long userId) {
        List<Connection> pendingRequests = connectionRepository.findPendingRequestsByUserId(userId);
        return pendingRequests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConnectionResponse> getSentRequests(Long userId) {
        List<Connection> sentRequests = connectionRepository.findSentPendingRequestsByUserId(userId);
        return sentRequests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean areUsersConnected(Long userId1, Long userId2) {
        return connectionRepository.areUsersConnected(userId1, userId2);
    }

    @Transactional(readOnly = true)
    public Long getConnectionsCount(Long userId) {
        return connectionRepository.countConnectionsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<User> getSuggestions(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("ID de usuario no puede ser nulo");
        }
        return userService.findSuggestionsForUser(userId);
    }

    private ConnectionResponse mapToResponse(Connection connection) {
        User user = connection.getUser();
        User connectedUser = connection.getConnectedUser();

        ConnectionResponse response = new ConnectionResponse();
        response.setId(connection.getId());
        response.setUserId(user.getId());
        response.setUserName(user.getFirstName() + " " + user.getLastName());
        response.setUserEmail(user.getEmail());
        response.setUserHeadline(user.getHeadline());
        response.setUserProfileImage(user.getProfileImageUrl());
        response.setConnectedUserId(connectedUser.getId());
        response.setConnectedUserName(connectedUser.getFirstName() + " " + connectedUser.getLastName());
        response.setConnectedUserEmail(connectedUser.getEmail());
        response.setConnectedUserHeadline(connectedUser.getHeadline());
        response.setConnectedUserProfileImage(connectedUser.getProfileImageUrl());
        response.setStatus(connection.getStatus().name().toLowerCase());
        response.setMessage(connection.getMessage());
        response.setRequestedAt(connection.getRequestedAt());
        response.setRespondedAt(connection.getRespondedAt());

        return response;
    }
}
