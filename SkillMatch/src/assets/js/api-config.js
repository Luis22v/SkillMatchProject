const API_CONFIG = {
    BASE_URL: window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? 'http://localhost:8080/api'
    : 'https://skillmatchproject-production.up.railway.app/api',
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

const API_BASE_URL = API_CONFIG.BASE_URL;

function saveToken(token) {
    if (token) localStorage.setItem('token', token);
}

function saveUserData(userData) {
    localStorage.setItem('userData', JSON.stringify(userData));
}

function getUserData() {
    const data = localStorage.getItem('userData');
    return data ? JSON.parse(data) : null;
}

function isAuthenticated() {
    const token = localStorage.getItem('token');
    if (!token) return false;
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.exp * 1000 > Date.now();
    } catch (_) {
        return false;
    }
}

async function logout() {
    const token = localStorage.getItem('token');
    try {
        await fetch(`${API_BASE_URL}/auth/logout`, {
            method: 'POST',
            credentials: 'include',
            headers: token ? { 'Authorization': `Bearer ${token}` } : {}
        });
    } catch (_) {}
    localStorage.removeItem('token');
    localStorage.removeItem('userData');
    localStorage.removeItem('companyOffers');
    localStorage.removeItem('skillmatch_saved_opportunities');

    const path = window.location.pathname;
    const isPublicPage = /index\.html|login|registro|seleccionar-registro/.test(path);
    if (!isPublicPage) {
        window.location.href = '../pages/index.html';
    }
}

async function fetchWithAuth(url, options = {}) {
    const token = localStorage.getItem('token');
    const response = await fetch(url, {
        ...options,
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
            ...options.headers
        }
    });

    return response;
}
