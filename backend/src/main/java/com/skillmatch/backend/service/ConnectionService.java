package com.skillmatch.backend.service;

import com.skillmatch.backend.dto.ConnectionRequest;
import com.skillmatch.backend.dto.ConnectionResponse;
import com.skillmatch.backend.exception.DuplicateResourceException;
import com.skillmatch.backend.exception.ResourceNotFoundException;
import com.skillmatch.backend.exception.UnauthorizedException;
import com.skillmatch.backend.model.Connection;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.repository.ConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

        // Verificar si ya existe una conexión
        if (connectionRepository.findConnectionBetweenUsers(userId, request.getConnectedUserId()).isPresent()) {
            throw new DuplicateResourceException("Ya existe una conexión o solicitud pendiente con este usuario");
        }
        
        Connection connection = new Connection();
        connection.setUser(user);
        connection.setConnectedUser(connectedUser);
        connection.setStatus("pending");
        connection.setMessage(request.getMessage());
        connection.setRequestedAt(LocalDateTime.now());
        
        connection = connectionRepository.save(connection);
        
        // Crear notificación para el usuario receptor
        notificationService.createConnectionRequestNotification(connectedUser.getId(), user.getId(), connection.getId());
        
        return mapToResponse(connection);
    }
    
    @Transactional
    public ConnectionResponse acceptConnection(Long connectionId, Long userId) {
        if (connectionId == null) {
            throw new IllegalArgumentException("ID de conexión no puede ser nulo");
        }
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de conexión no encontrada"));
        
        // Verificar que el usuario es el receptor de la solicitud
        if (!connection.getConnectedUser().getId().equals(userId)) {
            throw new UnauthorizedException("No tienes permiso para aceptar esta solicitud");
        }
        
        if (!"pending".equals(connection.getStatus())) {
            throw new RuntimeException("Esta solicitud ya fue procesada");
        }
        
        connection.setStatus("accepted");
        connection.setRespondedAt(LocalDateTime.now());
        connection = connectionRepository.save(connection);
        
        // Crear notificación para el usuario que envió la solicitud
        notificationService.createConnectionAcceptedNotification(connection.getUser().getId(), userId, connectionId);
        
        return mapToResponse(connection);
    }
    
    @Transactional
    public ConnectionResponse rejectConnection(Long connectionId, Long userId) {
        if (connectionId == null) {
            throw new IllegalArgumentException("ID de conexión no puede ser nulo");
        }
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de conexión no encontrada"));
        
        // Verificar que el usuario es el receptor de la solicitud
        if (!connection.getConnectedUser().getId().equals(userId)) {
            throw new UnauthorizedException("No tienes permiso para rechazar esta solicitud");
        }
        
        if (!"pending".equals(connection.getStatus())) {
            throw new RuntimeException("Esta solicitud ya fue procesada");
        }
        
        connection.setStatus("rejected");
        connection.setRespondedAt(LocalDateTime.now());
        connection = connectionRepository.save(connection);
        
        return mapToResponse(connection);
    }
    
    @Transactional
    public void blockConnection(Long connectionId, Long userId) {
        if (connectionId == null) {
            throw new IllegalArgumentException("ID de conexión no puede ser nulo");
        }
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Conexión no encontrada"));

        // Verificar que el usuario es parte de la conexión
        if (!connection.getUser().getId().equals(userId) && !connection.getConnectedUser().getId().equals(userId)) {
            throw new UnauthorizedException("No tienes permiso para bloquear esta conexión");
        }
        
        connection.setStatus("blocked");
        connectionRepository.save(connection);
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
        response.setStatus(connection.getStatus());
        response.setMessage(connection.getMessage());
        response.setRequestedAt(connection.getRequestedAt());
        response.setRespondedAt(connection.getRespondedAt());
        
        return response;
    }
}
