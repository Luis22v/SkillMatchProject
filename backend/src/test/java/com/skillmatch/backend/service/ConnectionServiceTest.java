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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.skillmatch.backend.model.ConnectionStatus.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class ConnectionServiceTest {

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ConnectionService connectionService;

    private User requester;
    private User receiver;
    private Connection pendingConnection;

    @BeforeEach
    void setUp() {
        requester = new User();
        requester.setId(1L);
        requester.setFirstName("Ana");
        requester.setLastName("García");
        requester.setEmail("ana@test.com");

        receiver = new User();
        receiver.setId(2L);
        receiver.setFirstName("Luis");
        receiver.setLastName("Martínez");
        receiver.setEmail("luis@test.com");

        pendingConnection = buildPendingConnection(10L);
    }

    // ─── sendConnectionRequest ────────────────────────────────────────────────

    @Test
    void sendConnectionRequest_happyPath_savesAndNotifies() {
        ConnectionRequest req = new ConnectionRequest(receiver.getId(), "Hola!");
        when(userService.getUserById(1L)).thenReturn(requester);
        when(userService.getUserById(2L)).thenReturn(receiver);
        when(connectionRepository.findConnectionBetweenUsers(1L, 2L)).thenReturn(Optional.empty());

        Connection saved = buildPendingConnection(10L);
        when(connectionRepository.save(any(Connection.class))).thenReturn(saved);

        ConnectionResponse response = connectionService.sendConnectionRequest(1L, req);

        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);
        verify(connectionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PENDING);
        assertThat(captor.getValue().getMessage()).isEqualTo("Hola!");

        verify(notificationService).createConnectionRequestNotification(
                eq(receiver.getId()), eq(requester.getId()), eq(saved.getId()));

        assertThat(response.getStatus()).isEqualTo("pending");
    }

    @Test
    void sendConnectionRequest_toSelf_throwsIllegalArgument() {
        ConnectionRequest req = new ConnectionRequest(1L, null);

        assertThatThrownBy(() -> connectionService.sendConnectionRequest(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ti mismo");
    }

    @Test
    void sendConnectionRequest_nullConnectedUserId_throwsIllegalArgument() {
        ConnectionRequest req = new ConnectionRequest(null, null);
        when(userService.getUserById(1L)).thenReturn(requester);

        assertThatThrownBy(() -> connectionService.sendConnectionRequest(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulo");
    }

    @Test
    void sendConnectionRequest_requesterNotFound_throwsResourceNotFound() {
        ConnectionRequest req = new ConnectionRequest(2L, null);
        when(userService.getUserById(1L)).thenThrow(new ResourceNotFoundException("Usuario no encontrado"));

        assertThatThrownBy(() -> connectionService.sendConnectionRequest(1L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sendConnectionRequest_receiverNotFound_throwsResourceNotFound() {
        ConnectionRequest req = new ConnectionRequest(2L, null);
        when(userService.getUserById(1L)).thenReturn(requester);
        when(userService.getUserById(2L)).thenThrow(new ResourceNotFoundException("Usuario no encontrado"));

        assertThatThrownBy(() -> connectionService.sendConnectionRequest(1L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sendConnectionRequest_alreadyExists_throwsDuplicate() {
        ConnectionRequest req = new ConnectionRequest(2L, null);
        when(userService.getUserById(1L)).thenReturn(requester);
        when(userService.getUserById(2L)).thenReturn(receiver);
        when(connectionRepository.findConnectionBetweenUsers(1L, 2L))
                .thenReturn(Optional.of(pendingConnection));

        assertThatThrownBy(() -> connectionService.sendConnectionRequest(1L, req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void sendConnectionRequest_noNotificationWhenSaveFails() {
        ConnectionRequest req = new ConnectionRequest(2L, null);
        when(userService.getUserById(1L)).thenReturn(requester);
        when(userService.getUserById(2L)).thenReturn(receiver);
        when(connectionRepository.findConnectionBetweenUsers(1L, 2L)).thenReturn(Optional.empty());
        when(connectionRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> connectionService.sendConnectionRequest(1L, req))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(notificationService);
    }

    // ─── acceptConnection ─────────────────────────────────────────────────────

    @Test
    void acceptConnection_happyPath_setsAcceptedAndNotifies() {
        when(connectionRepository.findById(10L)).thenReturn(Optional.of(pendingConnection));
        Connection saved = buildAcceptedConnection(10L);
        when(connectionRepository.save(any(Connection.class))).thenReturn(saved);

        ConnectionResponse response = connectionService.acceptConnection(10L, receiver.getId());

        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);
        verify(connectionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ACCEPTED);
        assertThat(captor.getValue().getRespondedAt()).isNotNull();

        verify(notificationService).createConnectionAcceptedNotification(
                eq(requester.getId()), eq(receiver.getId()), eq(10L));

        assertThat(response.getStatus()).isEqualTo("accepted");
    }

    @Test
    void acceptConnection_notFound_throwsResourceNotFound() {
        when(connectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectionService.acceptConnection(99L, receiver.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void acceptConnection_wrongUser_throwsUnauthorized() {
        when(connectionRepository.findById(10L)).thenReturn(Optional.of(pendingConnection));

        assertThatThrownBy(() -> connectionService.acceptConnection(10L, 99L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @ParameterizedTest
    @MethodSource("alreadyProcessedStatuses")
    void acceptConnection_alreadyProcessed_throwsIllegalArgument(ConnectionStatus status) {
        pendingConnection.setStatus(status);
        when(connectionRepository.findById(10L)).thenReturn(Optional.of(pendingConnection));

        assertThatThrownBy(() -> connectionService.acceptConnection(10L, receiver.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("procesada");
    }

    @Test
    void acceptConnection_nullConnectionId_throwsIllegalArgument() {
        assertThatThrownBy(() -> connectionService.acceptConnection(null, receiver.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptConnection_noNotificationWhenSaveFails() {
        when(connectionRepository.findById(10L)).thenReturn(Optional.of(pendingConnection));
        when(connectionRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> connectionService.acceptConnection(10L, receiver.getId()))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(notificationService);
    }

    // ─── rejectConnection ─────────────────────────────────────────────────────

    @Test
    void rejectConnection_happyPath_setsRejected() {
        when(connectionRepository.findById(10L)).thenReturn(Optional.of(pendingConnection));
        Connection saved = buildPendingConnection(10L);
        saved.setStatus(REJECTED);
        when(connectionRepository.save(any(Connection.class))).thenReturn(saved);

        ConnectionResponse response = connectionService.rejectConnection(10L, receiver.getId());

        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);
        verify(connectionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(REJECTED);
        assertThat(captor.getValue().getRespondedAt()).isNotNull();

        assertThat(response.getStatus()).isEqualTo("rejected");
        verifyNoInteractions(notificationService);
    }

    @Test
    void rejectConnection_notFound_throwsResourceNotFound() {
        when(connectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectionService.rejectConnection(99L, receiver.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectConnection_wrongUser_throwsUnauthorized() {
        when(connectionRepository.findById(10L)).thenReturn(Optional.of(pendingConnection));

        assertThatThrownBy(() -> connectionService.rejectConnection(10L, 99L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @ParameterizedTest
    @MethodSource("alreadyProcessedStatuses")
    void rejectConnection_alreadyProcessed_throwsIllegalArgument(ConnectionStatus status) {
        pendingConnection.setStatus(status);
        when(connectionRepository.findById(10L)).thenReturn(Optional.of(pendingConnection));

        assertThatThrownBy(() -> connectionService.rejectConnection(10L, receiver.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("procesada");
    }

    @Test
    void rejectConnection_nullConnectionId_throwsIllegalArgument() {
        assertThatThrownBy(() -> connectionService.rejectConnection(null, receiver.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── blockConnection ──────────────────────────────────────────────────────

    @Test
    void blockConnection_byRequester_setsBlocked() {
        when(connectionRepository.findById(10L)).thenReturn(Optional.of(pendingConnection));

        connectionService.blockConnection(10L, requester.getId());

        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);
        verify(connectionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BLOCKED);
    }

    @Test
    void blockConnection_byReceiver_setsBlocked() {
        when(connectionRepository.findById(10L)).thenReturn(Optional.of(pendingConnection));

        connectionService.blockConnection(10L, receiver.getId());

        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);
        verify(connectionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BLOCKED);
    }

    @Test
    void blockConnection_notFound_throwsResourceNotFound() {
        when(connectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectionService.blockConnection(99L, requester.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void blockConnection_thirdParty_throwsUnauthorized() {
        when(connectionRepository.findById(10L)).thenReturn(Optional.of(pendingConnection));

        assertThatThrownBy(() -> connectionService.blockConnection(10L, 99L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void blockConnection_nullConnectionId_throwsIllegalArgument() {
        assertThatThrownBy(() -> connectionService.blockConnection(null, requester.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── getMyConnections ─────────────────────────────────────────────────────

    @Test
    void getMyConnections_returnsMappedList() {
        Connection c1 = buildAcceptedConnection(11L);
        Connection c2 = buildAcceptedConnection(12L);
        when(connectionRepository.findAcceptedConnectionsByUserId(1L)).thenReturn(List.of(c1, c2));

        List<ConnectionResponse> result = connectionService.getMyConnections(1L);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> "accepted".equals(r.getStatus()));
    }

    @Test
    void getMyConnections_empty_returnsEmptyList() {
        when(connectionRepository.findAcceptedConnectionsByUserId(1L)).thenReturn(List.of());

        List<ConnectionResponse> result = connectionService.getMyConnections(1L);

        assertThat(result).isEmpty();
    }

    // ─── getPendingRequests / getSentRequests ─────────────────────────────────

    @Test
    void getPendingRequests_returnsMappedList() {
        List<Connection> pending = List.of(buildPendingConnection(20L), buildPendingConnection(21L));
        when(connectionRepository.findPendingRequestsByUserId(1L)).thenReturn(pending);

        List<ConnectionResponse> result = connectionService.getPendingRequests(1L);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> "pending".equals(r.getStatus()));
    }

    @Test
    void getPendingRequests_empty_returnsEmptyList() {
        when(connectionRepository.findPendingRequestsByUserId(1L)).thenReturn(List.of());

        assertThat(connectionService.getPendingRequests(1L)).isEmpty();
    }

    @Test
    void getSentRequests_returnsMappedList() {
        List<Connection> sent = List.of(buildPendingConnection(22L));
        when(connectionRepository.findSentPendingRequestsByUserId(1L)).thenReturn(sent);

        List<ConnectionResponse> result = connectionService.getSentRequests(1L);

        assertThat(result).hasSize(1);
        assertThat(result).allMatch(r -> "pending".equals(r.getStatus()));
    }

    @Test
    void getSentRequests_empty_returnsEmptyList() {
        when(connectionRepository.findSentPendingRequestsByUserId(1L)).thenReturn(List.of());

        assertThat(connectionService.getSentRequests(1L)).isEmpty();
    }

    // ─── areUsersConnected ────────────────────────────────────────────────────

    @Test
    void areUsersConnected_returnsTrue() {
        when(connectionRepository.areUsersConnected(1L, 2L)).thenReturn(true);

        assertThat(connectionService.areUsersConnected(1L, 2L)).isTrue();
    }

    @Test
    void areUsersConnected_returnsFalse() {
        when(connectionRepository.areUsersConnected(1L, 2L)).thenReturn(false);

        assertThat(connectionService.areUsersConnected(1L, 2L)).isFalse();
    }

    // ─── getConnectionsCount ─────────────────────────────────────────────────

    @Test
    void getConnectionsCount_returnsCount() {
        when(connectionRepository.countConnectionsByUserId(1L)).thenReturn(7L);

        assertThat(connectionService.getConnectionsCount(1L)).isEqualTo(7L);
    }

    // ─── getSuggestions ───────────────────────────────────────────────────────

    @Test
    void getSuggestions_returnsList() {
        when(userService.findSuggestionsForUser(1L)).thenReturn(List.of(receiver));

        List<User> result = connectionService.getSuggestions(1L);

        assertThat(result).containsExactly(receiver);
    }

    @Test
    void getSuggestions_nullUserId_throwsIllegalArgument() {
        assertThatThrownBy(() -> connectionService.getSuggestions(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulo");
    }

    // ─── MethodSources ────────────────────────────────────────────────────────

    static Stream<ConnectionStatus> alreadyProcessedStatuses() {
        return Stream.of(ACCEPTED, REJECTED, BLOCKED);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Connection buildPendingConnection(Long id) {
        Connection c = new Connection();
        c.setId(id);
        c.setUser(requester);
        c.setConnectedUser(receiver);
        c.setStatus(PENDING);
        c.setRequestedAt(LocalDateTime.now());
        return c;
    }

    private Connection buildAcceptedConnection(Long id) {
        Connection c = buildPendingConnection(id);
        c.setStatus(ACCEPTED);
        c.setRespondedAt(LocalDateTime.now());
        return c;
    }
}
