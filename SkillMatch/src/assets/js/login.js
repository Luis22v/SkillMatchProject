function showRecovery() {
    document.getElementById('loginForm').classList.remove('active');
    document.getElementById('recoveryFlow').classList.add('active');
}

function showLoginForm() {
    const rec = document.getElementById('recoveryFlow');
    if (rec) rec.classList.remove('active');
    document.getElementById('loginForm').classList.add('active');
}

document.querySelector('#loginForm form').addEventListener('submit', async function(e) {
    e.preventDefault();

    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const submitButton = e.target.querySelector('button[type="submit"]');

    if (!email || !password) {
        alert('Por favor, completa todos los campos');
        return;
    }

    try {
        submitButton.disabled = true;
        submitButton.textContent = 'Iniciando sesión...';

        const response = await fetch(`${API_CONFIG.BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            const text = await response.text();
            throw new Error('El servidor no devolvió una respuesta JSON válida');
        }

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Error al iniciar sesión');
        }

        saveUserData({
            id: data.id,
            email: data.email || email,
            firstName: data.firstName || '',
            lastName: data.lastName || '',
            role: data.role,
            companyId: data.companyId || null,
            expiresAt: data.expiresAt || null
        });

        const role = data.role;
        if (role === 'EMPRESA' || role === 'ROLE_EMPRESA') {
            window.location.href = 'perfil-empresa.html';
        } else if (role === 'USER' || role === 'ROLE_USER') {
            window.location.href = 'perfil-usuario.html';
        } else {
            alert('Error: Rol de usuario no reconocido');
        }

    } catch (error) {
        alert(error.message || 'Credenciales inválidas. Por favor, verifica tu email y contraseña.');
    } finally {
        submitButton.disabled = false;
        submitButton.textContent = 'Iniciar Sesión';
    }
});

(function () {
    const form = document.getElementById('form-recovery');
    if (!form) return;

    const steps = Array.from(form.querySelectorAll('.form-step'));
    const stepper = Array.from(form.querySelectorAll('.stepper .step'));
    let current = 0;
    let method = 'email';

    const setHidden = (el, hidden) => { if (el) el.classList.toggle('is-hidden', hidden); };

    const show = (i) => {
        steps.forEach((s, idx) => s.classList.toggle('active', idx === i));
        stepper.forEach((el, idx) => {
            el.classList.toggle('active', idx === i);
            el.classList.toggle('completed', idx < i);
        });
        current = i;
    };

    const validateStep = (i) => {
        const cont = steps[i];
        for (const f of cont.querySelectorAll('[required]')) {
            if (f.type === 'checkbox' && !f.checked) return false;
            if (!f.value || f.value.trim() === '') return false;
            if (f.pattern && !new RegExp('^' + f.pattern + '$').test(f.value)) return false;
        }
        return true;
    };

    const radios = form.querySelectorAll('input[name="rec-method"]');
    const phoneWrap = document.getElementById('wrap-phone');
    const phoneInput = document.getElementById('rec-phone');
    const emailWrap = document.getElementById('wrap-email');
    const emailInput = document.getElementById('rec-email');
    const emailHelp = emailInput ? emailInput.nextElementSibling : null;

    radios.forEach(r => r.addEventListener('change', () => {
        method = form.querySelector('input[name="rec-method"]:checked').value;
        const sms = method === 'sms';
        setHidden(phoneWrap, !sms);
        if (phoneInput) phoneInput.required = sms;
        setHidden(emailWrap, sms);
        if (emailInput) emailInput.required = !sms;
        if (emailHelp) emailHelp.textContent = sms
            ? 'También enviaremos notificación al correo.'
            : 'Te enviaremos un código a este correo.';
    }));

    form.querySelector('#rec-next-1').addEventListener('click', () => {
        if (method === 'sms') {
            const raw = (phoneInput?.value || '').replace(/\D/g, '');
            if (raw.length < 10) { alert('Ingresa un número de teléfono válido (10 dígitos).'); return; }
        }
        if (!validateStep(0)) { alert(method === 'sms' ? 'Completa el número de teléfono.' : 'Completa el correo.'); return; }
        alert('Hemos enviado un código a tu ' + (method === 'sms' ? 'teléfono (SMS) y correo' : 'correo'));
        show(1);
    });

    form.querySelector('#rec-prev-2').addEventListener('click', () => show(0));
    form.querySelector('#rec-next-2').addEventListener('click', () => {
        const code = document.getElementById('rec-code').value.trim();
        if (!/^\d{6}$/.test(code)) { alert('Ingresa el código de 6 dígitos.'); return; }
        show(2);
    });
    form.querySelector('#rec-prev-3').addEventListener('click', () => show(1));

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const p1 = document.getElementById('rec-pass').value;
        const p2 = document.getElementById('rec-pass2').value;
        if (p1.length < 8) { alert('La contraseña debe tener mínimo 8 caracteres.'); return; }
        if (p1 !== p2) { alert('Las contraseñas no coinciden.'); return; }
        alert('Tu contraseña ha sido restablecida. Ahora puedes iniciar sesión.');
        showLoginForm();
    });

    if (phoneInput) {
        phoneInput.addEventListener('input', () => {
            let digits = phoneInput.value.replace(/\D/g, '');
            if (digits.startsWith('57')) digits = digits.slice(2);
            if (digits.length > 10) digits = digits.slice(0, 10);
            const p1 = digits.slice(0, 3);
            const p2 = digits.slice(3, 6);
            const p3 = digits.slice(6, 10);
            let out = '+57 ';
            if (p1) out += p1;
            if (p2) out += ' ' + p2;
            if (p3) out += ' ' + p3;
            phoneInput.value = out.trim();
        });
    }
})();
