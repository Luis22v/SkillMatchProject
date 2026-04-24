// Script para la página de conexiones con backend integration

const API_BASE_URL = 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Iniciando página de conexiones...');
    
    // Verificar autenticación
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    // Cargar datos
    loadMyConnections();
    loadPendingRequests();
    loadSuggestions();
    
    // Funcionalidad de tabs
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const tabId = this.getAttribute('data-tab');
            
            // Remover clase active de todos los botones y contenidos
            tabBtns.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));
            
            // Agregar clase active al botón y contenido clickeado
            this.classList.add('active');
            document.getElementById(tabId).classList.add('active');
        });
    });
});

// Cargar mis conexiones
async function loadMyConnections() {
    const container = document.querySelector('#connections .connections-grid');
    if (!container) return;

    const token = localStorage.getItem('token');
    container.innerHTML = '<div class="loading-message">Cargando conexiones...</div>';

    try {
        const response = await fetch(`${API_BASE_URL}/connections/my-connections`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const connections = await response.json();
            console.log('✅ Conexiones cargadas:', connections.length);
            displayConnections(connections);
        } else {
            throw new Error('Error al cargar conexiones');
        }
    } catch (error) {
        console.error('❌ Error cargando conexiones:', error);
        container.innerHTML = '<div class="error-message">Error al cargar conexiones</div>';
    }
}

// Mostrar conexiones
function displayConnections(connections) {
    const container = document.querySelector('#connections .connections-grid');
    
    if (connections.length === 0) {
        container.innerHTML = '<div class="no-results-message"><p>Aún no tienes conexiones</p></div>';
        return;
    }

    container.innerHTML = connections.map(conn => {
        const otherUser = conn.requesterName ? 
            { id: conn.requesterId, name: conn.requesterName, image: conn.requesterImage } : 
            { id: conn.receiverId, name: conn.receiverName, image: conn.receiverImage };
        return `
            <div class="connection-card">
                <div class="connection-avatar">${otherUser.name ? otherUser.name.charAt(0) : '👤'}</div>
                <h3>${otherUser.name || 'Usuario'}</h3>
                <p class="connection-role">Conectado</p>
                <p class="connection-date">${new Date(conn.createdAt).toLocaleDateString('es-ES')}</p>
                <div class="connection-actions">
                    <button class="btn-primary" onclick="sendMessage(${otherUser.id})">💬 Mensaje</button>
                    <button class="btn-secondary" onclick="viewProfile(${otherUser.id})">Ver Perfil</button>
                </div>
            </div>
        `;
    }).join('');
}

// Cargar solicitudes pendientes
async function loadPendingRequests() {
    const container = document.querySelector('#solicitudes .connections-grid');
    if (!container) return;

    const token = localStorage.getItem('token');
    container.innerHTML = '<div class="loading-message">Cargando solicitudes...</div>';

    try {
        const response = await fetch(`${API_BASE_URL}/connections/pending-requests`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const requests = await response.json();
            console.log('✅ Solicitudes cargadas:', requests.length);
            displayRequests(requests);
        } else {
            throw new Error('Error al cargar solicitudes');
        }
    } catch (error) {
        console.error('❌ Error cargando solicitudes:', error);
        container.innerHTML = '<div class="error-message">Error al cargar solicitudes</div>';
    }
}

// Mostrar solicitudes
function displayRequests(requests) {
    const container = document.querySelector('#solicitudes .connections-grid');
    
    if (requests.length === 0) {
        container.innerHTML = '<div class="no-results-message"><p>No tienes solicitudes pendientes</p></div>';
        return;
    }

    container.innerHTML = requests.map(req => {
        return `
            <div class="connection-card">
                <div class="connection-avatar">${req.requesterName ? req.requesterName.charAt(0) : '👤'}</div>
                <h3>${req.requesterName || 'Usuario'}</h3>
                <p class="connection-message">"${req.message || 'Quiere conectar contigo'}"</p>
                <p class="connection-date">${new Date(req.createdAt).toLocaleDateString('es-ES')}</p>
                <div class="connection-actions">
                    <button class="btn-primary" onclick="acceptConnection(${req.id})">✓ Aceptar</button>
                    <button class="btn-secondary" onclick="rejectConnection(${req.id})">✗ Rechazar</button>
                </div>
            </div>
        `;
    }).join('');
}

// Cargar sugerencias
async function loadSuggestions() {
    const container = document.querySelector('#sugerencias .suggestions-grid');
    if (!container) return;

    const token = localStorage.getItem('token');
    container.innerHTML = '<div class="loading-message">Cargando sugerencias...</div>';

    try {
        const response = await fetch(`${API_BASE_URL}/connections/suggestions`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const suggestions = await response.json();
            console.log('✅ Sugerencias cargadas:', suggestions.length);
            displaySuggestions(suggestions);
        } else {
            throw new Error('Error al cargar sugerencias');
        }
    } catch (error) {
        console.error('❌ Error cargando sugerencias:', error);
        container.innerHTML = '<div class="error-message">Error al cargar sugerencias</div>';
    }
}

// Mostrar sugerencias
function displaySuggestions(suggestions) {
    const container = document.querySelector('#sugerencias .suggestions-grid');
    
    if (suggestions.length === 0) {
        container.innerHTML = '<div class="no-results-message"><p>No hay sugerencias disponibles</p></div>';
        return;
    }

    container.innerHTML = suggestions.map(user => `
        <div class="suggestion-card">
            <div class="suggestion-avatar">${user.firstName ? user.firstName.charAt(0) : '👤'}</div>
            <h3>${user.firstName} ${user.lastName || ''}</h3>
            <p class="suggestion-role">${user.role || 'Usuario'}</p>
            <p class="suggestion-skills">${user.skills ? user.skills.slice(0, 3).join(', ') : ''}</p>
            <div class="suggestion-actions">
                <button class="btn-primary" onclick="sendConnectionRequest(${user.id})">🤝 Conectar</button>
                <button class="btn-secondary" onclick="viewProfile(${user.id})">Ver Perfil</button>
            </div>
        </div>
    `).join('');
}

// Enviar solicitud de conexión
async function sendConnectionRequest(userId) {
    const token = localStorage.getItem('token');
    
    try {
        const response = await fetch(`${API_BASE_URL}/connections`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                connectedUserId: userId,
                message: '¡Me gustaría conectar contigo!'
            })
        });

        if (response.ok) {
            alert('✅ Solicitud de conexión enviada');
            loadSuggestions(); // Recargar
        } else {
            throw new Error('Error al enviar solicitud');
        }
    } catch (error) {
        console.error('❌ Error:', error);
        alert('Error al enviar solicitud');
    }
}

// Aceptar conexión
async function acceptConnection(connectionId) {
    const token = localStorage.getItem('token');
    
    try {
        const response = await fetch(`${API_BASE_URL}/connections/${connectionId}/accept`, {
            method: 'PATCH',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            alert('✅ Conexión aceptada');
            loadPendingRequests();
            loadMyConnections();
        } else {
            throw new Error('Error al aceptar conexión');
        }
    } catch (error) {
        console.error('❌ Error:', error);
        alert('Error al aceptar conexión');
    }
}

// Rechazar conexión
async function rejectConnection(connectionId) {
    const token = localStorage.getItem('token');
    
    try {
        const response = await fetch(`${API_BASE_URL}/connections/${connectionId}/reject`, {
            method: 'PATCH',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            alert('Conexión rechazada');
            loadPendingRequests();
        } else {
            throw new Error('Error al rechazar conexión');
        }
    } catch (error) {
        console.error('❌ Error:', error);
        alert('Error al rechazar conexión');
    }
}

// Enviar mensaje
function sendMessage(userId) {
    // TODO: Implementar cuando se cree mensajes.html
    alert('Función de mensajería en desarrollo');
}

// Ver perfil
function viewProfile(userId) {
    window.location.href = `perfil-usuario.html?id=${userId}`;
}
