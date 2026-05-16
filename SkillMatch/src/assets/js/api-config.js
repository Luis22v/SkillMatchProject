const API_CONFIG = {
    BASE_URL: 'http://localhost:8080/api',
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

const API_BASE_URL = 'http://localhost:8080/api';

// Token is stored as httpOnly cookie by the backend — JS cannot read it.
// saveToken is kept for API compatibility but is intentionally a no-op.
function saveToken(token) {}

function saveUserData(userData) {
    localStorage.setItem('userData', JSON.stringify(userData));
}

function getUserData() {
    const data = localStorage.getItem('userData');
    return data ? JSON.parse(data) : null;
}

function isAuthenticated() {
    const userData = getUserData();
    if (!userData) return false;
    if (userData.expiresAt) {
        return userData.expiresAt > Date.now();
    }
    return true;
}

async function logout() {
    try {
        await fetch(`${API_BASE_URL}/auth/logout`, {
            method: 'POST',
            credentials: 'include'
        });
    } catch (_) {}
    localStorage.removeItem('userData');
    localStorage.removeItem('companyOffers');
    localStorage.removeItem('skillmatch_saved_opportunities');
    window.location.href = '../pages/index.html';
}

async function fetchWithAuth(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            ...options.headers
        }
    });

    if (response.status === 401 || response.status === 403) {
        logout();
    }

    return response;
}
