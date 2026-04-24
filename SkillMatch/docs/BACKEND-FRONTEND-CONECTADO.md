# 🚀 Backend-Frontend Conectado - Guía Completa

## ✅ Estado Actual

### Backend
- **Estado**: ✅ Ejecutándose
- **Puerto**: 8080
- **URL Base**: http://localhost:8080/api
- **Java**: 21.0.8 (LTS)
- **Spring Boot**: 3.2.12
- **Base de Datos**: MySQL (conexión activa)

### Frontend
- **Ubicación**: `C:\Users\amaya\Desktop\PA\SkillMatch`
- **Configuración API**: Correcta (apuntando a localhost:8080)

---

## 📋 Endpoints Disponibles

### Autenticación (`/api/auth`)
- **POST** `/api/auth/register` - Registro de usuario
- **POST** `/api/auth/login` - Inicio de sesión

### Ejemplo de Registro:
```json
POST http://localhost:8080/api/auth/register
{
  "firstName": "Juan",
  "lastName": "Pérez",
  "email": "juan@example.com",
  "password": "Password123!",
  "phone": "3001234567"
}
```

### Ejemplo de Login:
```json
POST http://localhost:8080/api/auth/login
{
  "email": "juan@example.com",
  "password": "Password123!"
}
```

**Respuesta exitosa del login:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 1,
  "email": "juan@example.com",
  "firstName": "Juan",
  "lastName": "Pérez"
}
```

---

## 🔧 Cómo Usar

### 1. Verificar que el Backend esté Corriendo
Abre tu navegador y ve a:
```
http://localhost:8080
```
Si ves un error de autenticación o JSON, el backend está funcionando.

### 2. Probar la Conexión
Abre en tu navegador:
```
file:///C:/Users/amaya/Desktop/PA/SkillMatch/test-connection.html
```
Este archivo HTML prueba automáticamente la conexión con el backend.

### 3. Usar la Aplicación Frontend
Abre cualquiera de estas páginas:
- `file:///C:/Users/amaya/Desktop/PA/SkillMatch/src/pages/index.html` - Página principal
- `file:///C:/Users/amaya/Desktop/PA/SkillMatch/src/pages/registro-usuario.html` - Registro
- `file:///C:/Users/amaya/Desktop/PA/SkillMatch/src/pages/login-usuario.html` - Login

---

## 🛠️ Comandos Útiles

### Detener el Backend
En VS Code, ve al terminal donde corre el backend y presiona:
```
Ctrl + C
```

### Reiniciar el Backend
```powershell
cd c:\Users\amaya\Downloads\backend
.\mvnw.cmd spring-boot:run
```

### Verificar Estado de MySQL
```powershell
Get-Process -Name "*mysql*"
```

### Ver Logs del Backend en Tiempo Real
El terminal donde ejecutaste el backend muestra los logs en tiempo real.

---

## 🔐 Configuración de Seguridad

### CORS Configurado
El backend acepta peticiones desde:
- ✅ Todos los orígenes (`*`)
- ✅ Archivos locales (`file://`)
- ✅ localhost en cualquier puerto

### JWT Token
- **Expiración**: 24 horas (86400000 ms)
- **Header**: `Authorization: Bearer <token>`

---

## 🐛 Solución de Problemas

### ❌ Error: "Failed to fetch" o "CORS error"
**Solución**: 
1. Verifica que el backend esté corriendo: `http://localhost:8080`
2. Revisa los logs del backend en el terminal

### ❌ Error: "Connection refused"
**Solución**: 
1. El backend no está corriendo. Inícialo con:
   ```powershell
   cd c:\Users\amaya\Downloads\backend
   .\mvnw.cmd spring-boot:run
   ```

### ❌ Error: "Cannot connect to database"
**Solución**: 
1. Verifica que MySQL esté corriendo:
   ```powershell
   Get-Process -Name "*mysql*"
   ```
2. Si no está corriendo, inícialo desde XAMPP o tu gestor de MySQL

### ❌ Error: "401 Unauthorized" en endpoints protegidos
**Solución**: 
1. Asegúrate de incluir el token JWT en el header:
   ```javascript
   headers: {
     'Authorization': `Bearer ${token}`
   }
   ```

---

## 📁 Estructura de la Base de Datos

### Tablas Creadas Automáticamente:
- `users` - Información de usuarios
- `roles` - Roles del sistema
- `user_roles` - Relación usuarios-roles

### Base de Datos: `skillmatch`
- **Usuario**: root
- **Puerto**: 3306
- **Host**: localhost

---

## 🎯 Próximos Pasos

1. ✅ Backend funcionando en puerto 8080
2. ✅ CORS configurado
3. ✅ Conexión a MySQL activa
4. ✅ Frontend configurado para conectarse al backend
5. 📝 Probar registro e inicio de sesión
6. 📝 Implementar páginas de perfil y oportunidades
7. 📝 Agregar funcionalidad de búsqueda

---

## 📞 Testing con Postman o cURL

### Registro con cURL:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Juan",
    "lastName": "Pérez",
    "email": "juan@example.com",
    "password": "Password123!",
    "phone": "3001234567"
  }'
```

### Login con cURL:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@example.com",
    "password": "Password123!"
  }'
```

---

## 🌐 URLs Importantes

| Descripción | URL |
|-------------|-----|
| Backend Base | http://localhost:8080/api |
| Registro | http://localhost:8080/api/auth/register |
| Login | http://localhost:8080/api/auth/login |
| Frontend | file:///C:/Users/amaya/Desktop/PA/SkillMatch/src/pages/ |
| Test de Conexión | file:///C:/Users/amaya/Desktop/PA/SkillMatch/test-connection.html |

---

## ✨ Características Implementadas

- ✅ Autenticación JWT
- ✅ Registro de usuarios
- ✅ Inicio de sesión
- ✅ Encriptación de contraseñas (BCrypt)
- ✅ CORS habilitado
- ✅ Validación de datos
- ✅ Manejo de errores global
- ✅ Conexión a MySQL
- ✅ Java 21 LTS

---

**Última actualización**: 26 de noviembre de 2025
**Estado**: ✅ Backend y Frontend completamente funcionales y conectados
