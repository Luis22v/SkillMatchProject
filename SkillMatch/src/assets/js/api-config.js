// Configuración de la API
const API_CONFIG = {
    BASE_URL: 'http://localhost:8080/api', // Cambia el puerto según el que te asigne Spring Boot
    ENDPOINTS: {
        LOGIN: '/auth/login',
        REGISTRO_USUARIO: '/auth/register',
        REGISTRO_EMPRESA: '/auth/register',
        OPORTUNIDADES: '/oportunidades',
        USUARIOS: '/usuarios',
        EMPRESAS: '/empresas',
        JOBS: '/jobs',
        COMPANIES: '/companies',
        APPLICATIONS: '/applications',
        USERS: '/users',
        USER_PROFILE: (userId) => `/users/${userId}`,
        USER_SKILLS: (userId) => `/users/${userId}/skills`,
        USER_EXPERIENCES: (userId) => `/users/${userId}/experiences`,
        USER_EDUCATION: (userId) => `/users/${userId}/educations`
    }
};

// Función para obtener el token del localStorage
function getToken() {
    return localStorage.getItem('token');
}

// Función para guardar el token
function saveToken(token) { 
    localStorage.setItem('token', token);
}

// Función para guardar datos del usuario
function saveUserData(userData) {
    localStorage.setItem('userData', JSON.stringify(userData));
}

// Función para obtener datos del usuario
function getUserData() {
    const data = localStorage.getItem('userData');
    return data ? JSON.parse(data) : null;
}

// Función para verificar si el usuario está autenticado
function isAuthenticated() {
    return getToken() !== null;
}

// Función para cerrar sesión
function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('userData');
    window.location.href = '../pages/index.html';
}

// Función para hacer peticiones autenticadas
async function fetchWithAuth(url, options = {}) {
    const token = getToken();
    
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };
    
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    
    const response = await fetch(url, {
        ...options,
        headers
    });
    
    // Si el token expiró o es inválido, cerrar sesión
    if (response.status === 401 || response.status === 403) {
        logout();
    }
    
    return response;
}