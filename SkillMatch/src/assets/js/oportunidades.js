// Script para la página de oportunidades con backend integration

let allJobs = [];
let currentFilters = {};
let savedJobIds = new Set();

// Cargar oportunidades al iniciar
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Iniciando página de oportunidades...');
    loadJobs();
    loadSavedJobs();
    setupEventListeners();
});

// Cargar trabajos desde el backend
async function loadJobs() {
    const container = document.querySelector('.opportunities-list');
    if (!container) {
        console.error('❌ Contenedor .opportunities-list no encontrado');
        return;
    }

    try {
        container.innerHTML = '<div class="loading-message">Cargando oportunidades...</div>';
        
        console.log('🔍 Intentando cargar desde:', `${API_BASE_URL}/jobs`);
        const response = await fetchWithAuth(`${API_BASE_URL}/jobs`);
        
        console.log('📡 Respuesta recibida:', response.status);
        
        if (response.ok) {
            const data = await response.json();
            allJobs = Array.isArray(data) ? data : (data.content || []);
            console.log('✅ Oportunidades cargadas:', allJobs.length);
            if (allJobs.length > 0) {
                console.log('📋 Primera oportunidad:', allJobs[0]);
            }
            displayJobs(allJobs);
        } else {
            const errorText = await response.text();
            console.error('❌ Error HTTP:', response.status, errorText);
            throw new Error(`Error ${response.status}: ${errorText}`);
        }
    } catch (error) {
        console.error('❌ Error cargando oportunidades:', error);
        container.innerHTML = `
            <div class="error-message">
                <h3>Error al cargar oportunidades</h3>
                <p>${error.message || 'Por favor, intenta de nuevo más tarde'}</p>
                <button onclick="loadJobs()" class="btn-primary">Reintentar</button>
            </div>
        `;
    }
}

// Mostrar trabajos en el DOM
function displayJobs(jobs) {
    const container = document.querySelector('.opportunities-list');
    
    if (!jobs || jobs.length === 0) {
        container.innerHTML = `
            <div class="no-results-message">
                <h3>No se encontraron oportunidades</h3>
                <p>Intenta ajustar los filtros de búsqueda</p>
            </div>
        `;
        updateResultsCount(0);
        return;
    }

    container.innerHTML = jobs.map(job => createJobCard(job)).join('');
    updateResultsCount(jobs.length);
    attachJobCardListeners();
}

// Crear tarjeta de trabajo
function createJobCard(job) {
    const salary = job.salaryMin && job.salaryMax 
        ? `${formatCurrency(job.salaryMin)} - ${formatCurrency(job.salaryMax)}`
        : job.salaryMin 
        ? `Desde ${formatCurrency(job.salaryMin)}`
        : 'Salario a convenir';

    const skills = job.skills && job.skills.length > 0
        ? job.skills.slice(0, 4).map(skill => `<span class="skill">${skill}</span>`).join('')
        : '<span class="skill">Sin especificar</span>';

    const matchScore = calculateMatchScore(job);
    const matchClass = matchScore >= 80 ? 'match-high' : matchScore >= 60 ? 'match-medium' : 'match-low';

    const isSaved = savedJobIds.has(job.id);
    const saveButtonClass = isSaved ? 'btn-save saved' : 'btn-save';
    const saveButtonText = isSaved ? '❤️ Guardado' : '💾 Guardar';

    return `
        <div class="opportunity-card" data-job-id="${job.id}">
            <div class="card-header-opp">
                <div class="company-logo-opp">${job.companyName ? job.companyName.charAt(0) : '🏢'}</div>
                <div class="card-info">
                    <h3 class="job-title">${job.title}</h3>
                    <p class="company-name">${job.companyName || 'Empresa Confidencial'}</p>
                </div>
                <div class="match-badge ${matchClass}">${matchScore}% Match</div>
            </div>
            
            <div class="card-meta">
                <span class="meta-item">📍 ${job.location || 'Remoto'}</span>
                <span class="meta-item">💼 ${translateJobType(job.type)}</span>
                <span class="meta-item">🎓 ${translateExperienceLevel(job.experienceLevel)}</span>
                <span class="meta-item">💰 ${salary}</span>
            </div>
            
            <p class="card-description">${truncateText(job.description, 150)}</p>
            
            <div class="card-skills">
                ${skills}
            </div>
            
            <div class="card-footer">
                <span class="post-date">📅 ${formatDate(job.postedDate)}</span>
                <div class="card-actions">
                    <button class="${saveButtonClass}" data-job-id="${job.id}">${saveButtonText}</button>
                    <button class="btn-apply" data-job-id="${job.id}">Aplicar Ahora →</button>
                </div>
            </div>
        </div>
    `;
}

// Configurar event listeners
function setupEventListeners() {
    console.log('🔧 Configurando event listeners...');
    
    // Búsqueda
    const searchInput = document.getElementById('searchInput');
    const searchBtn = document.querySelector('.btn-search-main');
    
    if (searchInput && searchBtn) {
        searchBtn.addEventListener('click', performSearch);
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                performSearch();
            }
        });
        console.log('✓ Listeners de búsqueda configurados');
    }

    // Filtros
    const applyFilterBtn = document.querySelector('.btn-apply-filters');
    if (applyFilterBtn) {
        applyFilterBtn.addEventListener('click', applyFilters);
        console.log('✓ Botón de aplicar filtros configurado');
    }

    const clearFilterBtn = document.querySelector('.btn-clear-filters');
    if (clearFilterBtn) {
        clearFilterBtn.addEventListener('click', clearFilters);
        console.log('✓ Botón de limpiar filtros configurado');
    }

    // Rango de salario - actualización en tiempo real
    const salaryRange = document.getElementById('salaryRange');
    if (salaryRange) {
        salaryRange.addEventListener('input', function() {
            const value = parseInt(this.value);
            document.getElementById('maxSalary').textContent = formatCurrency(value);
        });
        console.log('✓ Rango de salario configurado');
    }

    // Ordenar
    const sortSelect = document.getElementById('sortSelect');
    if (sortSelect) {
        sortSelect.addEventListener('change', sortJobs);
        console.log('✓ Selector de ordenamiento configurado');
    }
}

// Adjuntar listeners a las tarjetas de trabajo
function attachJobCardListeners() {
    // Botones de aplicar
    document.querySelectorAll('.btn-apply').forEach(btn => {
        btn.addEventListener('click', function() {
            const jobId = this.dataset.jobId;
            applyToJob(jobId);
        });
    });

    // Botones de guardar
    document.querySelectorAll('.btn-save').forEach(btn => {
        btn.addEventListener('click', function() {
            const jobId = this.dataset.jobId;
            toggleSaveJob(jobId, this);
        });
    });
}

// Aplicar a trabajo
async function applyToJob(jobId) {
    const token = localStorage.getItem('token');
    const userData = JSON.parse(localStorage.getItem('userData') || '{}');
    
    console.log('🔍 Verificando autenticación:', { 
        hasToken: !!token, 
        userId: userData.id,
        userRole: userData.role,
        userData: userData
    });
    
    if (!token) {
        if (confirm('Debes iniciar sesión para aplicar a esta oportunidad.\n¿Deseas ir a la página de inicio de sesión?')) {
            window.location.href = 'login.html';
        }
        return;
    }
    
    // Verificar que sea un usuario (no una empresa)
    if (userData.role === 'EMPRESA' || userData.role === 'ADMIN') {
        alert('❌ Solo los usuarios pueden aplicar a ofertas de trabajo.\n\nLas empresas y administradores no pueden postularse.');
        return;
    }

    const job = allJobs.find(j => j.id == jobId);
    if (!job) {
        console.error('❌ Trabajo no encontrado:', jobId);
        return;
    }

    const confirmMessage = `¿Deseas aplicar a esta posición?\n\n📋 Puesto: ${job.title}\n🏢 Empresa: ${job.companyName || 'Empresa Confidencial'}\n📍 Ubicación: ${job.location || 'No especificada'}`;
    
    if (confirm(confirmMessage)) {
        try {
            console.log('📤 Enviando aplicación al trabajo:', jobId);
            
            const response = await fetchWithAuth(`${API_BASE_URL}/applications`, {
                method: 'POST',
                body: JSON.stringify({
                    jobId: parseInt(jobId),
                    coverLetter: `Estimado equipo, estoy muy interesado en la posición de ${job.title} y creo que mi perfil se alinea perfectamente con los requisitos.`
                })
            });
            
            if (response.ok) {
                const result = await response.json();
                console.log('✅ Aplicación enviada:', result);
                alert(`✅ ¡Aplicación enviada exitosamente!\n\n📋 Puesto: ${job.title}\n🏢 Empresa: ${job.companyName || 'Empresa Confidencial'}\n\nLa empresa revisará tu perfil pronto.`);
                
                // Deshabilitar el botón para evitar aplicaciones duplicadas
                const btn = document.querySelector(`.btn-apply[data-job-id="${jobId}"]`);
                if (btn) {
                    btn.disabled = true;
                    btn.textContent = '✓ Ya aplicaste';
                    btn.classList.add('applied');
                }
            } else {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Error al enviar aplicación');
            }
        } catch (error) {
            console.error('❌ Error aplicando al trabajo:', error);
            alert('❌ Error al enviar la aplicación: ' + error.message);
        }
    }
}

// Guardar/desguardar trabajo
function toggleSaveJob(btn) {
    const jobId = btn.dataset.jobId;
    
    if (btn.classList.contains('saved')) {
        // Desguardar
        btn.classList.remove('saved');
        btn.innerHTML = '💾 Guardar';
        console.log('🗑️ Trabajo removido de guardados:', jobId);
        
        // TODO: Implementar DELETE a /api/saved-jobs/:id
    } else {
        // Guardar
        btn.classList.add('saved');
        btn.innerHTML = '✓ Guardado';
        console.log('💾 Trabajo guardado:', jobId);
        
        // TODO: Implementar POST a /api/saved-jobs
        
        // Feedback visual
        btn.style.animation = 'pulse 0.5s ease';
        setTimeout(() => {
            btn.style.animation = '';
        }, 500);
    }
}

// Realizar búsqueda
async function performSearch() {
    const query = document.getElementById('searchInput').value.trim();
    
    if (!query) {
        displayJobs(allJobs);
        return;
    }

    console.log('🔍 Buscando:', query);

    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/jobs/search?keyword=${encodeURIComponent(query)}`);
        
        if (response.ok) {
            const jobs = await response.json();
            displayJobs(jobs);
        } else {
            displayJobs([]);
        }
    } catch (error) {
        console.error('Error en búsqueda:', error);
        displayJobs(allJobs);
    }
}

// Aplicar filtros
async function applyFilters() {
    const filters = {
        type: getCheckedValues('input[name="type"]:checked'),
        modality: getCheckedValues('input[name="modality"]:checked'),
        experienceLevel: getCheckedValues('input[name="experience"]:checked'),
        maxSalary: document.getElementById('salaryRange')?.value
    };

    console.log('🔧 Aplicando filtros:', filters);

    currentFilters = filters;

    // Construir query params
    const params = new URLSearchParams();
    if (filters.type.length > 0) params.append('type', filters.type[0]);
    if (filters.modality.length > 0) params.append('modality', filters.modality[0]);
    if (filters.experienceLevel.length > 0) params.append('experienceLevel', filters.experienceLevel[0]);
    if (filters.maxSalary) params.append('maxSalary', filters.maxSalary);

    try {
        const url = `${API_BASE_URL}/jobs/filter?${params.toString()}`;
        const response = await fetchWithAuth(url);
        
        if (response.ok) {
            const jobs = await response.json();
            displayJobs(jobs);
        } else {
            displayJobs([]);
        }
    } catch (error) {
        console.error('Error aplicando filtros:', error);
    }
}

// Limpiar filtros
function clearFilters() {
    console.log('🧹 Limpiando filtros...');
    
    // Desmarcar todos los checkboxes
    document.querySelectorAll('.filter-card input[type="checkbox"]').forEach(cb => {
        cb.checked = false;
    });
    
    // Resetear rango de salario
    const salaryRange = document.getElementById('salaryRange');
    if (salaryRange) {
        salaryRange.value = salaryRange.max;
        document.getElementById('maxSalary').textContent = formatCurrency(salaryRange.max);
    }

    // Limpiar filtros actuales
    currentFilters = {};
    
    // Recargar todas las oportunidades
    loadJobs();
    
    console.log('✅ Filtros limpiados');
}

// Ordenar trabajos
function sortJobs() {
    const sortValue = document.getElementById('sortSelect').value;
    let sorted = [...allJobs];

    switch(sortValue) {
        case 'recent':
            sorted.sort((a, b) => new Date(b.postedDate) - new Date(a.postedDate));
            break;
        case 'salary':
            sorted.sort((a, b) => (b.salaryMax || 0) - (a.salaryMax || 0));
            break;
        case 'match':
            sorted.sort((a, b) => calculateMatchScore(b) - calculateMatchScore(a));
            break;
    }

    displayJobs(sorted);
}

// Utilidades
function getCheckedValues(selector) {
    return Array.from(document.querySelectorAll(selector)).map(cb => cb.value);
}

function updateResultsCount(count) {
    const resultsCount = document.getElementById('resultsCount');
    if (resultsCount) {
        resultsCount.textContent = `Mostrando ${count} oportunidad${count !== 1 ? 'es' : ''}`;
    }
}

function translateJobType(type) {
    const types = {
        'empleo': 'Tiempo Completo',
        'practica': 'Práctica',
        'freelance': 'Freelance'
    };
    return types[type] || type;
}

function translateExperienceLevel(level) {
    const levels = {
        'sin-experiencia': 'Sin Experiencia',
        'junior': 'Junior',
        'semi-senior': 'Semi-Senior',
        'senior': 'Senior'
    };
    return levels[level] || level;
}

// Cargar trabajos guardados del usuario
async function loadSavedJobs() {
    const token = localStorage.getItem('token');
    if (!token) return;

    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/saved-jobs/job-ids`);

        if (response.ok) {
            const ids = await response.json();
            savedJobIds = new Set(ids);
            console.log('✅ Trabajos guardados cargados:', savedJobIds.size);
        }
    } catch (error) {
        console.error('❌ Error cargando trabajos guardados:', error);
    }
}

// Guardar o quitar trabajo de guardados
async function toggleSaveJob(jobId, buttonElement) {
    const token = localStorage.getItem('token');
    
    if (!token) {
        if (confirm('Debes iniciar sesión para guardar oportunidades.\n¿Deseas ir a la página de inicio de sesión?')) {
            window.location.href = 'login.html';
        }
        return;
    }

    const isSaved = savedJobIds.has(parseInt(jobId));
    
    try {
        if (isSaved) {
            // Quitar de guardados
            const response = await fetchWithAuth(`${API_BASE_URL}/saved-jobs/job/${jobId}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                savedJobIds.delete(parseInt(jobId));
                buttonElement.classList.remove('saved');
                buttonElement.textContent = '💾 Guardar';
                console.log('✅ Trabajo removido de guardados');
            }
        } else {
            // Agregar a guardados
            const response = await fetchWithAuth(`${API_BASE_URL}/saved-jobs`, {
                method: 'POST',
                body: JSON.stringify({ jobId: parseInt(jobId) })
            });

            if (response.ok) {
                savedJobIds.add(parseInt(jobId));
                buttonElement.classList.add('saved');
                buttonElement.textContent = '❤️ Guardado';
                console.log('✅ Trabajo guardado exitosamente');
            }
        }
    } catch (error) {
        console.error('❌ Error guardando/quitando trabajo:', error);
        alert('Error al actualizar el estado. Por favor intenta de nuevo.');
    }
}

function calculateMatchScore(job) {
    // Algoritmo simple de matching - puede mejorarse
    let score = 60; // Base score
    
    // Bonus por habilidades (requiere lógica más compleja con el perfil del usuario)
    if (job.skills && job.skills.length > 0) {
        score += Math.min(20, job.skills.length * 5);
    }
    
    // Bonus por modalidad remota
    if (job.modality === 'remoto' || job.modality === 'híbrido') {
        score += 10;
    }
    
    return Math.min(100, score);
}

console.log('✅ Módulo de oportunidades cargado');
