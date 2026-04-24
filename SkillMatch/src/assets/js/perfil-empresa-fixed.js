// SkillMatch - Perfil de Empresa
// Script para funcionalidad completa de la página

const API_BASE_URL = 'http://localhost:8080/api';
let currentOffers = [];
let currentCompanyData = {};

document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Iniciando perfil de empresa...');
    loadCompanyData();
    setupEventListeners();
    loadOffers();
});

// Cargar datos de la empresa desde localStorage
function loadCompanyData() {
    const userData = JSON.parse(localStorage.getItem('userData') || '{}');
    const token = localStorage.getItem('token');
    
    if (!token) {
        alert('Debes iniciar sesión para ver esta página');
        window.location.href = 'login-empresa.html';
        return;
    }
    
    currentCompanyData = userData;
    
    if (userData.firstName) {
        const companyNameElement = document.getElementById('companyName');
        if (companyNameElement) {
            companyNameElement.textContent = userData.firstName;
        }
    }
    
    console.log('✅ Datos de empresa cargados:', userData);
}

// Cargar ofertas desde localStorage
function loadOffers() {
    const savedOffers = JSON.parse(localStorage.getItem('companyOffers') || '[]');
    currentOffers = savedOffers;
    
    if (savedOffers.length > 0) {
        renderOffers(savedOffers);
    }
    console.log('📋 Ofertas cargadas:', savedOffers.length);
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
    currentOffers = currentOffers.filter((offer, index) => (offer.id || index) !== offerId);
    localStorage.setItem('companyOffers', JSON.stringify(currentOffers));
    renderOffers(currentOffers);
    showNotification('Oferta eliminada correctamente');
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
    if (offerIndex !== -1) {
        currentOffers[offerIndex].status = currentOffers[offerIndex].status === 'active' ? 'archived' : 'active';
        localStorage.setItem('companyOffers', JSON.stringify(currentOffers));
        renderOffers(currentOffers);
        showNotification(currentOffers[offerIndex].status === 'active' ? 'Oferta reabierta' : 'Oferta archivada');
    }
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
    const aboutText = document.querySelector('.section-card-body p')?.textContent || '';
    createModal('Editar Acerca de Nosotros', `
        <div class="form-group">
            <label for="editAboutText">Descripción de la empresa</label>
            <textarea id="editAboutText" rows="6" style="width:100%;padding:0.8rem;border:1px solid #e4e6e9;border-radius:8px;font-family:inherit;">${aboutText}</textarea>
            <small>Describe tu empresa, misión, visión y valores</small>
        </div>
    `, () => {
        const newText = document.getElementById('editAboutText').value;
        if (newText) {
            const aboutElement = document.querySelector('.section-card-body p');
            if (aboutElement) aboutElement.textContent = newText;
            currentCompanyData.about = newText;
            localStorage.setItem('userData', JSON.stringify(currentCompanyData));
            showNotification('Descripción actualizada correctamente');
            closeModal();
        }
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
        
        const typeLabels = { practice: 'Práctica', employment: 'Empleo', freelance: 'Freelance' };
        
        const offerData = {
            id: offerId !== null ? offerId : Date.now(),
            title,
            type,
            typeLabel: typeLabels[type],
            description,
            salary,
            location,
            publishDate: existingOffer?.publishDate || 'Hoy',
            candidates: existingOffer?.candidates || 0,
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
        showNotification(isEdit ? 'Oferta actualizada correctamente' : 'Oferta publicada correctamente');
        closeModal();
    });
}

function showCandidatesModal(offer) {
    console.log('👥 Abriendo modal candidatos');
    createModal(`Candidatos para: ${offer.title}`, `
        <p style="text-align:center;color:#65676b;padding:2rem;">
            <strong>${offer.candidates || 0}</strong> candidatos han aplicado a esta oferta.
        </p>
        <p style="text-align:center;color:#65676b;">
            Funcionalidad de gestión de candidatos próximamente.
        </p>
    `, null, 'Cerrar');
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

console.log('✅ perfil-empresa.js cargado correctamente');
