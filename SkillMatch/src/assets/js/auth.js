// Manejo del formulario de login (empresa y usuario)
document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('empresa-login-form')
                   || document.getElementById('usuario-login-form');

    if (!loginForm) return;

    loginForm.addEventListener('submit', async function(e) {
        e.preventDefault();

        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const loginBtn = document.getElementById('login-btn');
        const errorMessage = document.getElementById('error-message');
        const successMessage = document.getElementById('success-message');

        if (errorMessage) errorMessage.style.display = 'none';
        if (successMessage) successMessage.style.display = 'none';

        loginBtn.disabled = true;
        loginBtn.textContent = 'Iniciando sesión...';

        try {
            const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.LOGIN}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });

            if (response.ok) {
                const data = await response.json();

                saveToken(data.token);

                const role = data.role || 'USER';
                const isEmpresa = role === 'EMPRESA' || role === 'ROLE_EMPRESA';

                if (isEmpresa && !data.companyId) {
                    console.warn('⚠️ Usuario empresa sin companyId asociado');
                    if (errorMessage) {
                        errorMessage.textContent = 'Tu cuenta no tiene una empresa asociada. Contacta al administrador.';
                        errorMessage.style.display = 'block';
                    }
                    loginBtn.disabled = false;
                    loginBtn.textContent = 'Iniciar Sesión';
                    return;
                }

                saveUserData({
                    id: data.id,
                    companyId: data.companyId || null,
                    firstName: data.firstName,
                    lastName: data.lastName,
                    email: data.email,
                    role,
                    userType: isEmpresa ? 'empresa' : 'usuario'
                });

                if (successMessage) {
                    successMessage.textContent = '¡Inicio de sesión exitoso! Redirigiendo...';
                    successMessage.style.display = 'block';
                }

                setTimeout(() => {
                    window.location.href = isEmpresa ? 'perfil-empresa.html' : 'perfil-usuario.html';
                }, 1000);

            } else {
                let errorText = 'Credenciales inválidas. Por favor, verifica tu correo y contraseña.';
                try {
                    const errorData = await response.json();
                    errorText = errorData.message || errorText;
                } catch (e) {
                    // usar mensaje por defecto si no hay JSON
                }
                if (errorMessage) {
                    errorMessage.textContent = errorText;
                    errorMessage.style.display = 'block';
                }
                loginBtn.disabled = false;
                loginBtn.textContent = 'Iniciar Sesión';
            }

        } catch (error) {
            console.error('Error al iniciar sesión:', error);
            if (errorMessage) {
                errorMessage.textContent = 'Error de conexión. Por favor, verifica que el servidor esté corriendo e intenta de nuevo.';
                errorMessage.style.display = 'block';
            }
            loginBtn.disabled = false;
            loginBtn.textContent = 'Iniciar Sesión';
        }
    });
});
