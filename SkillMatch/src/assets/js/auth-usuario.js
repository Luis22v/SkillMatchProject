// Manejo del formulario de login de usuario
document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('usuario-login-form');
    
    if (!loginForm) return;
    
    loginForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const loginBtn = document.getElementById('login-btn');
        const errorMessage = document.getElementById('error-message');
        const successMessage = document.getElementById('success-message');
        
        // Ocultar mensajes anteriores
        if (errorMessage) errorMessage.style.display = 'none';
        if (successMessage) successMessage.style.display = 'none';
        
        // Deshabilitar botón mientras se procesa
        loginBtn.disabled = true;
        loginBtn.textContent = 'Iniciando sesión...';
        
        console.log('📤 Intentando login de usuario...');
        
        try {
            const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.LOGIN}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    email: email,
                    password: password
                })
            });
            
            console.log('📥 Respuesta del servidor:', response.status);
            
            if (response.ok) {
                const data = await response.json();
                console.log('✅ Login exitoso:', data);
                
                // Guardar token
                saveToken(data.token);
                
                // Determinar el tipo de usuario basado en el rol
                const role = data.role || 'USER';
                const isEmpresa = role === 'EMPRESA' || role === 'ROLE_EMPRESA';
                
                // Verificar companyId si es empresa
                if (isEmpresa && !data.companyId) {
                    console.warn('⚠️ ADVERTENCIA: Usuario empresa sin companyId asociado');
                    if (errorMessage) {
                        errorMessage.textContent = 'Tu cuenta no tiene una empresa asociada. Contacta al administrador.';
                        errorMessage.style.display = 'block';
                    }
                    loginBtn.disabled = false;
                    loginBtn.textContent = 'Iniciar Sesión';
                    return;
                }
                
                // Preparar datos del usuario
                const userData = {
                    id: data.id,
                    companyId: data.companyId || null,
                    firstName: data.firstName,
                    lastName: data.lastName,
                    email: data.email,
                    role: role,
                    userType: isEmpresa ? 'empresa' : 'usuario'
                };
                
                saveUserData(userData);
                console.log('💾 Datos guardados:', userData);
                
                // Mostrar mensaje de éxito
                if (successMessage) {
                    successMessage.textContent = '¡Inicio de sesión exitoso! Redirigiendo...';
                    successMessage.style.display = 'block';
                }
                
                // Redirigir según el tipo de usuario
                setTimeout(() => {
                    if (isEmpresa) {
                        console.log('🏢 Redirigiendo a perfil de empresa...');
                        window.location.href = 'perfil-empresa.html';
                    } else {
                        console.log('👤 Redirigiendo a perfil de usuario...');
                        window.location.href = 'perfil-usuario.html';
                    }
                }, 1000);
                
            } else {
                let errorText = 'Credenciales inválidas. Por favor, verifica tu correo y contraseña.';
                try {
                    const errorData = await response.json();
                    errorText = errorData.message || errorText;
                } catch (e) {
                    // Si no puede parsear JSON, usar el mensaje por defecto
                }
                if (errorMessage) {
                    errorMessage.textContent = errorText;
                    errorMessage.style.display = 'block';
                }
                
                // Rehabilitar botón
                loginBtn.disabled = false;
                loginBtn.textContent = 'Iniciar Sesión';
            }
            
        } catch (error) {
            console.error('Error al iniciar sesión:', error);
            if (errorMessage) {
                errorMessage.textContent = 'Error de conexión. Por favor, verifica que el servidor esté corriendo e intenta de nuevo.';
                errorMessage.style.display = 'block';
            }
            
            // Rehabilitar botón
            loginBtn.disabled = false;
            loginBtn.textContent = 'Iniciar Sesión';
        }
    });
});