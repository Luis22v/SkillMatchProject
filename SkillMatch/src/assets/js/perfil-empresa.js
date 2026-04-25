// SkillMatch - Perfil de Empresa
// Script para funcionalidad completa de la página

const API_BASE_URL = typeof API_CONFIG !== 'undefined' ? API_CONFIG.BASE_URL : 'http://localhost:8080/api';
let currentOffers = [];
let currentCompanyData = {};

document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Iniciando perfil de empresa...');
    loadCompanyData();
    setupEventListeners();
    loadOffers();
    loadActivityStats();
});

// Cargar datos de la empresa desde localStorage y backend
function loadCompanyData() {
    const userData = JSON.parse(localStorage.getItem('userData') || '{}');
    const token = localStorage.getItem('token');
    
    if (!token) {
        alert('Debes iniciar sesión para ver esta página');
        window.location.href = 'login-empresa.html';
        return;
    }
    
    // Validar que exista companyId
    if (!userData.companyId) {
        console.error('❌ CRÍTICO: No se encontró companyId en userData');
        alert('⚠️ Tu cuenta no tiene una empresa asociada.\n\nPor favor contacta al administrador o cierra sesión e inicia sesión nuevamente.');
        return;
    }
    
    currentCompanyData = userData;
    
    // Actualizar nombre de la empresa
    if (userData.firstName) {
        const companyNameElement = document.getElementById('companyName');
        if (companyNameElement) {
            companyNameElement.textContent = userData.firstName;
        }
    }
    
    console.log('✅ Datos de empresa cargados:', userData);
    console.log('🏢 Company ID:', userData.companyId);
    
    // Cargar información completa de la empresa desde el backend
    if (userData.companyId) {
        fetchWithAuth(`${API_BASE_URL}/companies/${userData.companyId}`)
        .then(response => {
            if (!response.ok) throw new Error('Empresa no encontrada');
            return response.json();
        })
        .then(company => {
            console.log('📋 Información de empresa desde backend:', company);
            
            // Actualizar descripción "Acerca de"
            const aboutElement = document.getElementById('companyAbout');
            if (aboutElement && company.description) {
                aboutElement.innerHTML = `<p>${company.description}</p>`;
            } else if (aboutElement) {
                aboutElement.innerHTML = `<p>Esta empresa aún no ha agregado una descripción. Haz clic en el botón de editar para agregar información sobre tu empresa.</p>`;
            }
            
            // Actualizar otros datos si existen
            if (company.industry) {
                const industryElement = document.getElementById('companyIndustry');
                if (industryElement) industryElement.textContent = company.industry;
            }
            
            // Guardar datos completos
            currentCompanyData = { ...userData, ...company };
        })
        .catch(error => {
            console.error('❌ Error cargando datos de empresa:', error);
            const aboutElement = document.getElementById('companyAbout');
            if (aboutElement) {
                aboutElement.innerHTML = `<p>Esta empresa aún no ha agregado una descripción. Haz clic en el botón de editar para agregar información sobre tu empresa.</p>`;
            }
        });
    }
}

// Cargar estadísticas de actividad desde backend
function loadActivityStats() {
    const userData = JSON.parse(localStorage.getItem('userData') || '{}');

    if (!userData.companyId) {
        console.log('⚠️ No hay ID de usuario para cargar estadísticas');
        return;
    }

    // Cargar estadísticas reales desde el endpoint de backend
    fetchWithAuth(`${API_BASE_URL}/companies/${userData.companyId}/statistics`)
    .then(response => {
        if (!response.ok) throw new Error('Error al cargar estadísticas');
        return response.json();
    })
    .then(stats => {
        console.log('📊 Estadísticas reales cargadas:', stats);
        
        // Actualizar estadísticas REALES en el DOM
        const statOfertasPublicadas = document.getElementById('statOfertasPublicadas');
        const statCandidatosContactados = document.getElementById('statCandidatosContactados');
        const statContratacionesExitosas = document.getElementById('statContratacionesExitosas');
        const statMiembrosEquipo = document.getElementById('statMiembrosEquipo');
        
        if (statOfertasPublicadas) statOfertasPublicadas.textContent = stats.totalJobs || 0;
        if (statCandidatosContactados) statCandidatosContactados.textContent = stats.totalApplications || 0;
        if (statContratacionesExitosas) statContratacionesExitosas.textContent = stats.activeJobs || 0;
        if (statMiembrosEquipo) statMiembrosEquipo.textContent = stats.pendingApplications || 0;
        
        // Agregar más estadísticas si hay elementos en el HTML
        const profileViews = document.getElementById('profile-views');
        const responseRate = document.getElementById('response-rate');
        
        if (profileViews) profileViews.textContent = stats.profileViews || 0;
        if (responseRate) responseRate.textContent = `${(stats.responseRate || 0).toFixed(1)}%`;
        
        console.log('✅ Estadísticas actualizadas con datos reales del backend');
    })
    .catch(error => {
        console.error('❌ Error cargando estadísticas:', error);
        // Valores por defecto en caso de error
        document.getElementById('statOfertasPublicadas').textContent = '0';
        document.getElementById('statCandidatosContactados').textContent = '0';
        document.getElementById('statContratacionesExitosas').textContent = '0';
        document.getElementById('statMiembrosEquipo').textContent = '0';
    });
}

// Cargar ofertas desde backend
function loadOffers() {
    const userData = JSON.parse(localStorage.getItem('userData') || '{}');

    if (!userData.companyId) {
        console.log('⚠️ No se encontró companyId, cargando ofertas almacenadas localmente...');
        const savedOffers = JSON.parse(localStorage.getItem('companyOffers') || '[]');
        currentOffers = savedOffers;
        if (savedOffers.length > 0) {
            renderOffers(savedOffers);
        }
        return;
    }

    // Cargar desde backend
    fetchWithAuth(`${API_BASE_URL}/jobs/company/${userData.companyId}`)
    .then(response => {
        if (!response.ok) throw new Error('Error al cargar ofertas');
        return response.json();
    })
    .then(async data => {
        // Transformar datos del backend al formato del frontend

        // Obtener el conteo de candidatos para cada oferta
        const offersWithCandidates = await Promise.all(data.map(async job => {
            let candidateCount = 0;
            try {
                const countResponse = await fetchWithAuth(`${API_BASE_URL}/applications/job/${job.id}/count`);
                if (countResponse.ok) {
                    candidateCount = await countResponse.json();
                }
            } catch (error) {
                console.error(`Error obteniendo candidatos para job ${job.id}:`, error);
            }
            
            return {
                id: job.id,
                title: job.title,
                type: job.type?.toLowerCase() || 'employment',
                typeLabel: job.type || 'Empleo',
                description: job.description,
                salary: job.salaryRange,
                location: job.location,
                publishDate: new Date(job.createdAt).toLocaleDateString('es-ES'),
                candidates: candidateCount,
                status: (job.status?.toLowerCase() === 'abierta' || job.active) ? 'active' : 'archived'
            };
        }));
        
        currentOffers = offersWithCandidates;
        renderOffers(currentOffers);
        console.log('📋 Ofertas cargadas desde backend:', currentOffers.length);
    })
    .catch(error => {
        console.error('❌ Error cargando ofertas:', error);
        // Fallback a localStorage
        const savedOffers = JSON.parse(localStorage.getItem('companyOffers') || '[]');
        currentOffers = savedOffers;
        if (savedOffers.length > 0) {
            renderOffers(savedOffers);
        }
    });
}

// Renderizar ofertas
function renderOffers(offers) {
    const offersList = document.getElementById('offersList');
    if (!offersList) return;
    
    if (offers.length === 0) {
        offersList.innerHTML = '<p style="text-align:center;color:#65676b;padding:2rem;">No hay ofertas publicadas. ¡Crea tu primera oferta!</p>';
        return;
    }
    
    offersList.innerHTML = offers.map((offer, index) => {
        const offerId = offer.id || index;
        return `
        <div class="offer-card" data-offer-id="${offerId}">
            <div class="offer-card-header">
                <div class="offer-title-section">
                    <h3>${offer.title}</h3>
                    <span class="offer-type-badge ${offer.type}">${offer.typeLabel}</span>
                </div>
                <button class="offer-menu-btn" onclick="showOfferMenu(${offerId})" title="Opciones">⋮</button>
            </div>
            <div class="offer-card-info">
                <div class="offer-info-row">
                    <span class="info-label">Candidatos:</span>
                    <span class="info-value">${offer.candidates || 0} interesados</span>
                </div>
                <div class="offer-info-row">
                    <span class="info-label">Publicado:</span>
                    <span class="info-value">${offer.publishDate || 'Hoy'}</span>
                </div>
                <div class="offer-info-row">
                    <span class="info-label">Estado:</span>
                    <span class="status-badge ${offer.status === 'active' ? 'active' : 'inactive'}">${offer.status === 'active' ? 'Activa' : 'Archivada'}</span>
                </div>
            </div>
            <div class="offer-card-actions">
                <button class="btn-offer-action primary" onclick="viewOfferCandidates(${offerId})">Ver Candidatos</button>
                <button class="btn-offer-action" onclick="editOffer(${offerId})">Editar</button>
                <button class="btn-offer-action ${offer.status === 'active' ? '' : 'success'}" onclick="toggleOfferStatus(${offerId})">
                    ${offer.status === 'active' ? 'Archivar' : 'Reabrir'}
                </button>
            </div>
        </div>
    `;
    }).join('');
}

// ========================================
// EVENT LISTENERS
// ========================================

function setupEventListeners() {
    console.log('🔧 Configurando event listeners...');
    
    // Botón Cerrar Sesión
    const btnCerrarSesion = document.getElementById('btnCerrarSesion');
    if (btnCerrarSesion) {
        btnCerrarSesion.addEventListener('click', function(e) {
            e.preventDefault();
            if (confirm('¿Estás seguro de que deseas cerrar sesión?')) {
                localStorage.removeItem('token');
                localStorage.removeItem('userData');
                window.location.href = 'login.html';
            }
        });
        console.log('✅ Botón cerrar sesión configurado');
    }
    
    // Botón Editar Portada
    const btnEditCover = document.getElementById('btnEditCover');
    if (btnEditCover) {
        btnEditCover.addEventListener('click', openChangeCoverModal);
        console.log('✅ Botón editar portada configurado');
    }
    
    // Botón Editar Logo
    const btnEditLogo = document.getElementById('btnEditLogo');
    if (btnEditLogo) {
        btnEditLogo.addEventListener('click', openChangeLogoModal);
        console.log('✅ Botón editar logo configurado');
    }
    
    // Botón Editar Perfil
    const btnEditProfile = document.getElementById('btnEditProfile');
    if (btnEditProfile) {
        btnEditProfile.addEventListener('click', openEditProfileModal);
        console.log('✅ Botón editar perfil configurado');
    }
    
    // Botón Editar "Acerca de"
    const btnEditAbout = document.getElementById('btnEditAbout');
    if (btnEditAbout) {
        btnEditAbout.addEventListener('click', openEditAboutModal);
        console.log('✅ Botón editar acerca de configurado');
    }
    
    // Botón Nueva Oferta
    const btnNewOffer = document.getElementById('btnNewOffer');
    if (btnNewOffer) {
        btnNewOffer.addEventListener('click', () => openNewOfferModal());
        console.log('✅ Botón nueva oferta configurado');
    }
    
    // Botón Upgrade Plan
    const btnUpgradePlan = document.querySelector('.btn-upgrade-plan');
    if (btnUpgradePlan) {
        btnUpgradePlan.addEventListener('click', openUpgradePlanModal);
        console.log('✅ Botón upgrade plan configurado');
    }
    
    console.log('✅ Todos los event listeners configurados');
}

// ========================================
// FUNCIONES DE OFERTAS
// ========================================

function showOfferMenu(offerId) {
    console.log('🗑️ Mostrar menú para oferta:', offerId);
    if (confirm('¿Deseas eliminar esta oferta?')) {
        deleteOffer(offerId);
    }
}

function deleteOffer(offerId) {
    console.log('🗑️ Eliminando oferta:', offerId);

    // Eliminar en backend
    fetchWithAuth(`${API_BASE_URL}/jobs/${offerId}`, { method: 'DELETE' })
    .then(response => {
        if (!response.ok) throw new Error('Error al eliminar oferta');
        return response.json();
    })
    .then(data => {
        currentOffers = currentOffers.filter((offer, index) => (offer.id || index) !== offerId);
        localStorage.setItem('companyOffers', JSON.stringify(currentOffers));
        renderOffers(currentOffers);
        showNotification('Oferta eliminada correctamente');
        console.log('✅ Oferta eliminada del backend');
    })
    .catch(error => {
        console.error('❌ Error eliminando oferta:', error);
        // Fallback: eliminar solo de localStorage
        currentOffers = currentOffers.filter((offer, index) => (offer.id || index) !== offerId);
        localStorage.setItem('companyOffers', JSON.stringify(currentOffers));
        renderOffers(currentOffers);
        showNotification('Oferta eliminada localmente');
    });
}

function viewOfferCandidates(offerId) {
    console.log('👥 Ver candidatos para oferta:', offerId);
    const offer = currentOffers.find((o, i) => (o.id || i) === offerId);
    if (offer) {
        showCandidatesModal(offer);
    }
}

function editOffer(offerId) {
    console.log('✏️ Editar oferta:', offerId);
    const offer = currentOffers.find((o, i) => (o.id || i) === offerId);
    if (offer) {
        openNewOfferModal(offer, offerId);
    }
}

function toggleOfferStatus(offerId) {
    console.log('🔄 Toggle status oferta:', offerId);
    const offerIndex = currentOffers.findIndex((o, i) => (o.id || i) === offerId);
    if (offerIndex === -1) return;
    
    const newStatus = currentOffers[offerIndex].status === 'active' ? 'archived' : 'active';

    // Actualizar en backend
    fetchWithAuth(`${API_BASE_URL}/jobs/${offerId}/status?status=${newStatus.toUpperCase()}`, { method: 'PATCH' })
    .then(response => {
        if (!response.ok) throw new Error('Error al cambiar estado');
        return response.json();
    })
    .then(data => {
        currentOffers[offerIndex].status = newStatus;
        localStorage.setItem('companyOffers', JSON.stringify(currentOffers));
        renderOffers(currentOffers);
        showNotification(newStatus === 'active' ? 'Oferta reabierta' : 'Oferta archivada');
        console.log('✅ Estado actualizado en backend');
    })
    .catch(error => {
        console.error('❌ Error actualizando estado:', error);
        // Fallback: actualizar solo localmente
        currentOffers[offerIndex].status = newStatus;
        localStorage.setItem('companyOffers', JSON.stringify(currentOffers));
        renderOffers(currentOffers);
        showNotification('Estado actualizado localmente');
    });
}

// ========================================
// MODALES
// ========================================

function openChangeCoverModal() {
    console.log('📷 Abriendo modal cambiar portada');
    const currentCover = document.getElementById('companyCover')?.src || '';
    createModal('Cambiar Portada', `
        <div class="form-group">
            <label for="newCoverUrl">URL de la imagen de portada</label>
            <input type="url" id="newCoverUrl" placeholder="https://ejemplo.com/imagen.jpg" value="${currentCover}">
            <small>Recomendado: 1200x300px</small>
        </div>
    `, () => {
        const newUrl = document.getElementById('newCoverUrl').value;
        if (newUrl) {
            document.getElementById('companyCover').src = newUrl;
            currentCompanyData.coverUrl = newUrl;
            localStorage.setItem('userData', JSON.stringify(currentCompanyData));
            showNotification('Portada actualizada correctamente');
            closeModal();
        }
    });
}

function openChangeLogoModal() {
    console.log('🖼️ Abriendo modal cambiar logo');
    const currentLogo = document.getElementById('companyLogo')?.src || '';
    createModal('Cambiar Logo', `
        <div class="form-group">
            <label for="newLogoUrl">URL del logo</label>
            <input type="url" id="newLogoUrl" placeholder="https://ejemplo.com/logo.png" value="${currentLogo}">
            <small>Recomendado: 120x120px</small>
        </div>
    `, () => {
        const newUrl = document.getElementById('newLogoUrl').value;
        if (newUrl) {
            document.getElementById('companyLogo').src = newUrl;
            currentCompanyData.logoUrl = newUrl;
            localStorage.setItem('userData', JSON.stringify(currentCompanyData));
            showNotification('Logo actualizado correctamente');
            closeModal();
        }
    });
}

function openEditProfileModal() {
    console.log('👤 Abriendo modal editar perfil');
    const companyIndustry = document.getElementById('companyIndustry')?.textContent || '';
    createModal('Editar Perfil de Empresa', `
        <div class="form-group">
            <label for="editCompanyName">Nombre de la Empresa</label>
            <input type="text" id="editCompanyName" value="${currentCompanyData.firstName || ''}">
        </div>
        <div class="form-group">
            <label for="editCompanyIndustry">Sector / Industria</label>
            <input type="text" id="editCompanyIndustry" value="${companyIndustry}">
        </div>
        <div class="form-group">
            <label for="editCompanyEmail">Email</label>
            <input type="email" id="editCompanyEmail" value="${currentCompanyData.email || ''}">
        </div>
        <div class="form-group">
            <label for="editCompanyPhone">Teléfono</label>
            <input type="tel" id="editCompanyPhone" value="${currentCompanyData.phone || ''}">
        </div>
    `, () => {
        const newName = document.getElementById('editCompanyName').value;
        const newIndustry = document.getElementById('editCompanyIndustry').value;
        const newEmail = document.getElementById('editCompanyEmail').value;
        const newPhone = document.getElementById('editCompanyPhone').value;
        
        if (newName) {
            const nameElement = document.getElementById('companyName');
            const industryElement = document.getElementById('companyIndustry');
            if (nameElement) nameElement.textContent = newName;
            if (industryElement) industryElement.textContent = newIndustry;
            
            currentCompanyData.firstName = newName;
            currentCompanyData.email = newEmail;
            currentCompanyData.phone = newPhone;
            
            localStorage.setItem('userData', JSON.stringify(currentCompanyData));
            showNotification('Perfil actualizado correctamente');
            closeModal();
        }
    });
}

function openEditAboutModal() {
    console.log('📝 Abriendo modal editar acerca de');
    const aboutElement = document.getElementById('companyAbout');
    const aboutText = aboutElement?.querySelector('p')?.textContent || '';
    
    createModal('Editar Acerca de Nosotros', `
        <div class="form-group">
            <label for="editAboutText">Descripción de la empresa</label>
            <textarea id="editAboutText" rows="6" style="width:100%;padding:0.8rem;border:1px solid #e4e6e9;border-radius:8px;font-family:inherit;">${aboutText}</textarea>
            <small>Describe tu empresa, misión, visión y valores</small>
        </div>
    `, () => {
        const newText = document.getElementById('editAboutText').value.trim();
        if (!newText) {
            alert('La descripción no puede estar vacía');
            return;
        }
        
        const userData = JSON.parse(localStorage.getItem('userData') || '{}');
        const token = localStorage.getItem('token');
        
        if (!userData.companyId || !token) {
            alert('Error: No se encontró información de sesión');
            return;
        }
        
        // Actualizar en el backend con PATCH para actualización parcial
        fetchWithAuth(`${API_BASE_URL}/companies/${userData.companyId}/description`, {
            method: 'PATCH',
            body: JSON.stringify({
                description: newText
            })
        })
        .then(response => {
            if (!response.ok) {
                return response.json().then(err => {
                    throw new Error(err.message || 'Error al actualizar descripción');
                });
            }
            return response.json();
        })
        .then(data => {
            console.log('✅ Descripción actualizada en backend:', data);
            
            // Actualizar UI
            const aboutElement = document.getElementById('companyAbout');
            if (aboutElement) {
                aboutElement.innerHTML = `<p>${newText}</p>`;
            }
            
            // Actualizar datos locales
            currentCompanyData.description = newText;
            
            showNotification('Descripción actualizada correctamente');
            closeModal();
        })
        .catch(error => {
            console.error('❌ Error actualizando descripción:', error);
            console.error('Detalles del error:', error.message);
            alert(`Error al guardar la descripción: ${error.message}`);
        });
    });
}

function openNewOfferModal(existingOffer = null, offerId = null) {
    const isEdit = existingOffer !== null;
    console.log(isEdit ? '✏️ Abriendo modal editar oferta' : '➕ Abriendo modal nueva oferta');
    
    createModal(isEdit ? 'Editar Oferta' : 'Nueva Oferta de Empleo', `
        <div class="form-group">
            <label for="offerTitle">Título de la oferta *</label>
            <input type="text" id="offerTitle" placeholder="ej. Desarrollador Frontend Senior" value="${existingOffer?.title || ''}" required>
        </div>
        <div class="form-group">
            <label for="offerType">Tipo de oferta *</label>
            <select id="offerType" required>
                <option value="practice" ${existingOffer?.type === 'practice' ? 'selected' : ''}>Práctica</option>
                <option value="employment" ${existingOffer?.type === 'employment' ? 'selected' : ''}>Empleo</option>
                <option value="freelance" ${existingOffer?.type === 'freelance' ? 'selected' : ''}>Freelance</option>
            </select>
        </div>
        <div class="form-group">
            <label for="offerDescription">Descripción *</label>
            <textarea id="offerDescription" rows="4" placeholder="Describe las responsabilidades y requisitos..." required>${existingOffer?.description || ''}</textarea>
        </div>
        <div class="form-group">
            <label for="offerSalary">Rango Salarial (opcional)</label>
            <input type="text" id="offerSalary" placeholder="ej. $2M - $3M / mes" value="${existingOffer?.salary || ''}">
        </div>
        <div class="form-group">
            <label for="offerLocation">Ubicación *</label>
            <input type="text" id="offerLocation" placeholder="ej. Cartagena de Indias" value="${existingOffer?.location || 'Cartagena de Indias'}" required>
        </div>
    `, () => {
        const title = document.getElementById('offerTitle').value;
        const type = document.getElementById('offerType').value;
        const description = document.getElementById('offerDescription').value;
        const salary = document.getElementById('offerSalary').value;
        const location = document.getElementById('offerLocation').value;
        
        if (!title || !type || !description || !location) {
            alert('Por favor completa todos los campos obligatorios (*)');
            return;
        }
        
        const typeLabels = { practice: 'práctica', employment: 'empleo', freelance: 'freelance' };
        const token = localStorage.getItem('token');
        const userData = JSON.parse(localStorage.getItem('userData') || '{}');
        
        // Preparar datos para el backend
        console.log('📋 Datos de usuario completos:', userData);
        
        if (!userData.companyId) {
            console.error('❌ companyId no encontrado en userData');
            alert('⚠️ No se encontró el identificador de la empresa.\n\nPor favor, cierra sesión e inicia sesión nuevamente para sincronizar tus datos.');
            return;
        }

        const backendData = {
            companyId: userData.companyId,
            title,
            description,
            type: typeLabels[type],
            location,
            modality: 'híbrido',
            experienceLevel: 'junior' // Valor por defecto si no se especifica
        };
        
        // Agregar salario si existe
        if (salary) {
            const salaryMatch = salary.match(/\$?([\d.,]+)\s*-?\s*\$?([\d.,]+)?/);
            if (salaryMatch) {
                backendData.salaryMin = parseFloat(salaryMatch[1].replace(/[.,]/g, '')) || 0;
                backendData.salaryMax = salaryMatch[2] ? parseFloat(salaryMatch[2].replace(/[.,]/g, '')) : backendData.salaryMin;
            }
        }
        
        // Determinar endpoint y método
        const url = isEdit ? `${API_BASE_URL}/jobs/${offerId}` : `${API_BASE_URL}/jobs`;
        const method = isEdit ? 'PUT' : 'POST';
        
        console.log('📤 Enviando oferta al backend:');
        console.log('   URL:', url);
        console.log('   Método:', method);
        console.log('   Datos:', backendData);
        console.log('🔑 Token:', token ? '✓ presente' : '✗ AUSENTE');
        console.log('👤 User ID:', userData.id);
        console.log('🏢 Company ID:', userData.companyId);
        
        // Guardar en backend
        fetchWithAuth(url, {
            method: method,
            body: JSON.stringify(backendData)
        })
        .then(async response => {
            console.log('📥 Respuesta del servidor:', response.status, response.statusText);
            
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: 'Error desconocido' }));
                console.error('❌ Error del servidor:', errorData);
                throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('✅ Oferta guardada en backend:', data);
            
            // Actualizar lista local
            const offerData = {
                id: data.id,
                title: data.title,
                type: data.type?.toLowerCase() || 'employment',
                typeLabel: data.type || 'Empleo',
                description: data.description,
                salary: data.salaryRange,
                location: data.location,
                publishDate: new Date(data.createdAt).toLocaleDateString('es-ES'),
                candidates: 0,
                status: 'active'
            };
            
            if (isEdit) {
                const index = currentOffers.findIndex((o, i) => (o.id || i) === offerId);
                if (index !== -1) {
                    currentOffers[index] = offerData;
                }
            } else {
                currentOffers.unshift(offerData);
            }
            
            localStorage.setItem('companyOffers', JSON.stringify(currentOffers));
            renderOffers(currentOffers);
            showNotification(isEdit ? 'Oferta actualizada en la base de datos' : 'Oferta publicada en la base de datos');
            closeModal();
        })
        .catch(error => {
            console.error('❌ Error guardando oferta:', error);
            console.error('Detalles completos:', error.message);
            alert('Error al guardar la oferta:\n\n' + error.message + '\n\nRevisa la consola (F12) para más detalles.');
        });
    });
}

async function showCandidatesModal(offer) {
    console.log('👥 Abriendo modal candidatos para oferta:', offer.id);

    // Mostrar modal con loading
    const modal = createModal(`Candidatos para: ${offer.title}`, `
        <div style="text-align:center;padding:2rem;">
            <p>Cargando candidatos...</p>
        </div>
    `);
    
    // Ocultar botones de acción mientras carga
    const modalActions = modal.querySelector('.modal-actions');
    if (modalActions) modalActions.style.display = 'none';
    
    try {
        // Obtener aplicaciones para esta oferta
        const response = await fetchWithAuth(`${API_BASE_URL}/applications/job/${offer.id}`);
        
        if (!response.ok) {
            throw new Error('Error al cargar candidatos');
        }
        
        const applications = await response.json();
        console.log('✅ Candidatos cargados:', applications.length);
        
        // Generar contenido del modal
        let content = '';
        
        if (applications.length === 0) {
            content = `
                <div style="text-align:center;padding:2rem;">
                    <p style="color:#65676b;font-size:1.1rem;">No hay candidatos para esta oferta todavía.</p>
                </div>
            `;
        } else {
            content = `
                <div style="max-height:500px;overflow-y:auto;">
                    <p style="margin-bottom:1.5rem;color:#65676b;">
                        <strong>${applications.length}</strong> candidato${applications.length !== 1 ? 's' : ''} ha${applications.length !== 1 ? 'n' : ''} aplicado a esta oferta.
                    </p>
                    ${applications.map(app => createCandidateCard(app, offer.id)).join('')}
                </div>
            `;
        }
        
        // Actualizar contenido del modal
        const modalBody = modal.querySelector('.modal-body');
        if (modalBody) {
            modalBody.innerHTML = content;
        }
        
        // Mostrar botón de cerrar
        if (modalActions) {
            modalActions.style.display = 'flex';
            modalActions.innerHTML = '<button class="btn-secondary-modal" onclick="closeModal()">Cerrar</button>';
        }
        
    } catch (error) {
        console.error('❌ Error cargando candidatos:', error);
        const modalBody = modal.querySelector('.modal-body');
        if (modalBody) {
            modalBody.innerHTML = `
                <div style="text-align:center;padding:2rem;">
                    <p style="color:#dc3545;">Error al cargar los candidatos. Por favor, intenta de nuevo.</p>
                </div>
            `;
        }
        
        // Mostrar botón de cerrar
        if (modalActions) {
            modalActions.style.display = 'flex';
            modalActions.innerHTML = '<button class="btn-secondary-modal" onclick="closeModal()">Cerrar</button>';
        }
    }
}

function createCandidateCard(application, offerId) {
    const appliedDate = new Date(application.appliedDate).toLocaleDateString('es-ES', {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
    });
    
    const statusColors = {
        'pendiente': '#FFA500',
        'revisada': '#1ba3a3',
        'aceptada': '#28a745',
        'rechazada': '#dc3545'
    };
    
    const statusLabels = {
        'pendiente': 'Pendiente',
        'revisada': 'Revisada',
        'aceptada': 'Aceptada',
        'rechazada': 'Rechazada'
    };
    
    const status = application.status?.toLowerCase() || 'pendiente';
    
    return `
        <div style="border:1px solid #e4e6e9;border-radius:12px;padding:1.5rem;margin-bottom:1rem;background:white;box-shadow:0 2px 8px rgba(0,0,0,0.05);">
            <div style="display:flex;gap:1.5rem;margin-bottom:1.5rem;">
                <div style="flex-shrink:0;">
                    <img src="${application.userProfileImageUrl || '../assets/img/default-avatar.png'}" 
                         alt="${application.userName}" 
                         style="width:80px;height:80px;border-radius:50%;object-fit:cover;border:3px solid #1ba3a3;">
                </div>
                <div style="flex:1;">
                    <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:0.5rem;">
                        <div>
                            <h4 style="margin:0 0 0.5rem 0;color:#003d82;font-size:1.2rem;">${application.userName || 'Candidato'}</h4>
                            <p style="margin:0 0 0.5rem 0;color:#666;font-size:1rem;font-weight:500;">${application.userHeadline || 'Profesional'}</p>
                        </div>
                        <span style="padding:0.5rem 1rem;border-radius:20px;font-size:0.875rem;font-weight:600;color:white;background-color:${statusColors[status]};">
                            ${statusLabels[status]}
                        </span>
                    </div>
                    <div style="display:flex;flex-direction:column;gap:0.4rem;margin-top:0.8rem;">
                        <p style="margin:0;color:#555;font-size:0.9rem;">
                            <strong>📧 Email:</strong> <a href="mailto:${application.userEmail}" style="color:#1ba3a3;text-decoration:none;">${application.userEmail || 'No especificado'}</a>
                        </p>
                        <p style="margin:0;color:#555;font-size:0.9rem;">
                            <strong>📱 Teléfono:</strong> ${application.userPhone || 'No especificado'}
                        </p>
                        <p style="margin:0;color:#555;font-size:0.9rem;">
                            <strong>📍 Ubicación:</strong> ${application.userLocation || 'No especificada'}
                        </p>
                        <p style="margin:0;color:#666;font-size:0.875rem;margin-top:0.5rem;">
                            📅 Aplicó el ${appliedDate}
                        </p>
                    </div>
                </div>
            </div>
            ${application.coverLetter ? `
                <div style="background:#f8f9fa;padding:1rem;border-radius:8px;margin-bottom:1rem;border-left:3px solid #1ba3a3;">
                    <p style="margin:0 0 0.3rem 0;color:#003d82;font-weight:600;font-size:0.9rem;">Carta de presentación:</p>
                    <p style="margin:0;color:#333;font-size:0.9rem;line-height:1.6;">${application.coverLetter}</p>
                </div>
            ` : ''}
            <div style="display:flex;gap:0.75rem;flex-wrap:wrap;margin-top:1.5rem;padding-top:1rem;border-top:1px solid #e4e6e9;">
                <button onclick="viewCandidateProfile(${application.userId})" 
                        style="padding:0.6rem 1.2rem;background:#003d82;color:white;border:none;border-radius:8px;cursor:pointer;font-size:0.9rem;font-weight:500;display:flex;align-items:center;gap:0.5rem;">
                    👤 Ver Perfil Completo
                </button>
                <button onclick="contactCandidateByEmail('${application.userEmail}', '${application.userName}')" 
                        style="padding:0.6rem 1.2rem;background:#1ba3a3;color:white;border:none;border-radius:8px;cursor:pointer;font-size:0.9rem;font-weight:500;display:flex;align-items:center;gap:0.5rem;">
                    ✉️ Contactar
                </button>
                ${status === 'pendiente' ? `
                    <button onclick="updateCandidateStatus(${application.id}, 'aceptada', ${offerId})" 
                            style="padding:0.6rem 1.2rem;background:#28a745;color:white;border:none;border-radius:8px;cursor:pointer;font-size:0.9rem;font-weight:500;">
                        ✓ Aceptar
                    </button>
                    <button onclick="updateCandidateStatus(${application.id}, 'rechazada', ${offerId})" 
                            style="padding:0.6rem 1.2rem;background:#dc3545;color:white;border:none;border-radius:8px;cursor:pointer;font-size:0.9rem;font-weight:500;">
                        ✗ Rechazar
                    </button>
                ` : ''}
            </div>
        </div>
    `;
}

// Función para contactar candidato por email
function contactCandidateByEmail(email, candidateName) {
    const companyName = currentCompanyData.firstName || 'Nuestra empresa';
    const subject = encodeURIComponent(`Oportunidad laboral en ${companyName}`);
    const body = encodeURIComponent(`Hola ${candidateName},\n\nNos ha interesado tu perfil y nos gustaría ponernos en contacto contigo para discutir una oportunidad laboral.\n\nSaludos,\n${companyName}`);
    window.location.href = `mailto:${email}?subject=${subject}&body=${body}`;
}

async function updateCandidateStatus(applicationId, newStatus, offerId) {
    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/applications/${applicationId}/status`, {
            method: 'PUT',
            body: JSON.stringify({ status: newStatus })
        });
        
        if (response.ok) {
            showNotification(`Candidato ${newStatus === 'aceptada' ? 'aceptado' : 'rechazado'} exitosamente`);
            // Recargar modal con candidatos actualizados
            const offer = currentOffers.find(o => o.id === offerId);
            if (offer) {
                setTimeout(() => showCandidatesModal(offer), 500);
            }
        } else {
            throw new Error('Error al actualizar estado');
        }
    } catch (error) {
        console.error('❌ Error actualizando estado:', error);
        alert('Error al actualizar el estado del candidato');
    }
}

function openUpgradePlanModal() {
    console.log('💎 Abriendo modal upgrade plan');
    createModal('Upgrade a Plan Premium', `
        <div style="text-align:center;padding:1rem;">
            <h3 style="color:#1ba3a3;margin-bottom:1rem;">Plan Premium</h3>
            <p style="font-size:2rem;font-weight:700;color:#0f2f53;margin:1rem 0;">$99.000/mes</p>
            <ul style="text-align:left;max-width:300px;margin:1.5rem auto;list-style:none;padding:0;">
                <li style="padding:0.5rem 0;">✓ Ofertas ilimitadas</li>
                <li style="padding:0.5rem 0;">✓ Mensajería directa con candidatos</li>
                <li style="padding:0.5rem 0;">✓ Analytics avanzado</li>
                <li style="padding:0.5rem 0;">✓ Destacar ofertas</li>
                <li style="padding:0.5rem 0;">✓ Soporte prioritario 24/7</li>
            </ul>
        </div>
    `, () => {
        showNotification('Redirigiendo a la pasarela de pago...');
        closeModal();
    }, 'Actualizar Plan');
}

function viewCandidateProfile(candidateId) {
    console.log('👤 Ver perfil candidato:', candidateId);
    showNotification('Abriendo perfil del candidato...');
    setTimeout(() => {
        window.location.href = 'perfil-usuario.html';
    }, 1000);
}

function contactCandidate(candidateId, candidateName) {
    console.log('💬 Contactar candidato:', candidateId);
    showNotification(`Abriendo chat con ${candidateName}...`);
}

// ========================================
// UTILIDADES
// ========================================

function createModal(title, content, onSave = null, saveButtonText = 'Guardar') {
    const existingModal = document.getElementById('genericModal');
    if (existingModal) {
        existingModal.remove();
    }
    
    const modal = document.createElement('div');
    modal.id = 'genericModal';
    modal.className = 'modal';
    modal.style.display = 'flex';
    modal.innerHTML = `
        <div class="modal-content" style="max-width:600px;width:90%;">
            <div class="modal-header">
                <h2>${title}</h2>
                <button class="modal-close" onclick="closeModal()">&times;</button>
            </div>
            <div class="modal-body">
                ${content}
            </div>
            <div class="modal-actions">
                <button class="btn-secondary-modal" onclick="closeModal()">Cancelar</button>
                ${onSave ? `<button class="btn-primary-modal" id="modalSaveBtn">${saveButtonText}</button>` : ''}
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    if (onSave) {
        document.getElementById('modalSaveBtn').addEventListener('click', onSave);
    }
    
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            closeModal();
        }
    });
    
    return modal;
}

function closeModal() {
    const modal = document.getElementById('genericModal');
    if (modal) {
        modal.remove();
    }
}

function showNotification(message) {
    console.log('📢 Notificación:', message);
    const notification = document.createElement('div');
    notification.className = 'custom-notification';
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: linear-gradient(135deg, #1ba3a3, #0f7e7e);
        color: white;
        padding: 16px 24px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 10000;
        font-weight: 500;
        animation: slideInRight 0.3s ease;
        max-width: 400px;
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'slideOutRight 0.3s ease';
        setTimeout(() => {
            if (notification.parentNode) {
                document.body.removeChild(notification);
            }
        }, 300);
    }, 3000);
}

// Cargar aplicaciones recibidas por la empresa
async function loadApplications() {
    const userData = JSON.parse(localStorage.getItem('userData') || '{}');
    const token = localStorage.getItem('token');
    
    if (!userData.companyId || !token) {
        console.log('⚠️ No hay companyId o token para cargar aplicaciones');
        return;
    }
    
    try {
        console.log('📥 Cargando aplicaciones para companyId:', userData.companyId);
        
        const response = await fetchWithAuth(`${API_BASE_URL}/applications/company/${userData.companyId}`);
        
        if (response.ok) {
            const applications = await response.json();
            console.log('✅ Aplicaciones recibidas:', applications.length);
            displayApplications(applications);
        } else {
            console.error('❌ Error cargando aplicaciones:', response.status);
        }
    } catch (error) {
        console.error('❌ Error cargando aplicaciones:', error);
    }
}

// Normalizar estado (español -> inglés)
function normalizeStatus(status) {
    const statusMap = {
        'pendiente': 'pending',
        'revisada': 'reviewed',
        'aceptada': 'accepted',
        'rechazada': 'rejected'
    };
    return statusMap[status.toLowerCase()] || status.toLowerCase();
}

// Mostrar aplicaciones en la interfaz
function displayApplications(applications) {
    // Buscar o crear sección de aplicaciones
    let applicationsSection = document.getElementById('applications-section');
    
    if (!applicationsSection) {
        // Crear sección si no existe
        const profileContent = document.querySelector('.company-profile-content');
        if (!profileContent) return;
        
        applicationsSection = document.createElement('section');
        applicationsSection.id = 'applications-section';
        applicationsSection.className = 'info-section';
        applicationsSection.innerHTML = `
            <div class="section-header">
                <h2><span class="section-icon">📬</span> Aplicaciones Recibidas</h2>
            </div>
            <div id="applications-list" class="applications-container"></div>
        `;
        
        // Insertar después de la sección de ofertas
        const offersSection = document.getElementById('ofertas-section');
        if (offersSection) {
            offersSection.parentNode.insertBefore(applicationsSection, offersSection.nextSibling);
        } else {
            profileContent.appendChild(applicationsSection);
        }
    }
    
    const applicationsList = document.getElementById('applications-list');
    
    if (applications.length === 0) {
        applicationsList.innerHTML = `
            <div class="empty-state">
                <p>📭 No has recibido aplicaciones todavía</p>
            </div>
        `;
        return;
    }
    
    // Normalizar estados y agrupar
    const grouped = {
        pending: applications.filter(app => normalizeStatus(app.status) === 'pending'),
        reviewed: applications.filter(app => normalizeStatus(app.status) === 'reviewed'),
        accepted: applications.filter(app => normalizeStatus(app.status) === 'accepted'),
        rejected: applications.filter(app => normalizeStatus(app.status) === 'rejected')
    };
    
    applicationsList.innerHTML = `
        <div class="applications-stats">
            <div class="stat-card">
                <span class="stat-number">${grouped.pending.length}</span>
                <span class="stat-label">Pendientes</span>
            </div>
            <div class="stat-card">
                <span class="stat-number">${grouped.reviewed.length}</span>
                <span class="stat-label">Revisadas</span>
            </div>
            <div class="stat-card">
                <span class="stat-number">${grouped.accepted.length}</span>
                <span class="stat-label">Aceptadas</span>
            </div>
            <div class="stat-card">
                <span class="stat-number">${grouped.rejected.length}</span>
                <span class="stat-label">Rechazadas</span>
            </div>
        </div>
        
        <div class="applications-list">
            ${applications.map(app => createApplicationCard(app)).join('')}
        </div>
    `;
}

// Crear tarjeta de aplicación
function createApplicationCard(application) {
    const normalizedStatus = normalizeStatus(application.status);
    
    const statusColors = {
        pending: '#FFA500',
        reviewed: '#1ba3a3',
        accepted: '#28a745',
        rejected: '#dc3545'
    };
    
    const statusLabels = {
        pending: 'Pendiente',
        reviewed: 'Revisada',
        accepted: 'Aceptada',
        rejected: 'Rechazada'
    };
    
    const appliedDate = new Date(application.appliedDate).toLocaleDateString('es-ES', {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
    });
    
    return `
        <div class="application-card" data-application-id="${application.id}">
            <div class="application-header">
                <div class="candidate-info">
                    <h4>${application.userName || 'Candidato'}</h4>
                    <p class="job-title">${application.jobTitle || 'Sin título'}</p>
                </div>
                <span class="application-status" style="background-color: ${statusColors[normalizedStatus]}">
                    ${statusLabels[normalizedStatus]}
                </span>
            </div>
            <p class="application-date">📅 Aplicó el ${appliedDate}</p>
            ${application.coverLetter ? `
                <p class="cover-letter-preview">${application.coverLetter.substring(0, 150)}${application.coverLetter.length > 150 ? '...' : ''}</p>
            ` : ''}
            <div class="application-actions">
                <button class="btn-view-profile" onclick="viewCandidateProfile(${application.userId})">Ver Perfil</button>
                ${normalizedStatus === 'pending' ? `
                    <button class="btn-accept" onclick="updateApplicationStatus(${application.id}, 'aceptada')">✓ Aceptar</button>
                    <button class="btn-reject" onclick="updateApplicationStatus(${application.id}, 'rechazada')">✗ Rechazar</button>
                ` : ''}
            </div>
        </div>
    `;
}

// Actualizar estado de aplicación
async function updateApplicationStatus(applicationId, newStatus) {
    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/applications/${applicationId}/status`, {
            method: 'PUT',
            body: JSON.stringify({ status: newStatus })
        });
        
        if (response.ok) {
            showNotification(`Aplicación ${newStatus === 'accepted' ? 'aceptada' : 'rechazada'} exitosamente`);
            loadApplications(); // Recargar aplicaciones
        } else {
            throw new Error('Error al actualizar estado');
        }
    } catch (error) {
        console.error('❌ Error actualizando estado:', error);
        alert('Error al actualizar el estado de la aplicación');
    }
}

// Ver perfil del candidato - IMPLEMENTADO
function viewCandidateProfile(userId) {
    console.log('👤 Abriendo perfil del candidato:', userId);
    // Abrir perfil en nueva pestaña
    window.open(`perfil-usuario.html?id=${userId}`, '_blank');
}

// Cargar aplicaciones al cargar la página
document.addEventListener('DOMContentLoaded', function() {
    // Dar tiempo para que se carguen los datos de la empresa
    setTimeout(() => {
        loadApplications();
    }, 1000);
});

console.log('✅ perfil-empresa.js cargado correctamente');
