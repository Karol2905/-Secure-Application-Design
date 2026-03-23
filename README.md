# 🔒 Secure App Workshop
**Enterprise Architecture Workshop — TDSE**

Aplicación web segura con autenticación JWT desplegada en AWS EC2 usando Docker. Apache actúa como reverse proxy con TLS (Let's Encrypt), Spring Boot maneja la API REST y PostgreSQL almacena contraseñas hasheadas con BCrypt.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-green)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)
![TLS](https://img.shields.io/badge/TLS-Let's%20Encrypt-brightgreen)
![AWS](https://img.shields.io/badge/AWS-EC2%20AL2023-yellow)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)

---

## 📐 Arquitectura

```
Browser (HTTPS :443)
       │
       ▼
┌──────────────────────────────────────────────────────┐
│  EC2 — Amazon Linux 2023                             │
│  TU_DOMINIO.duckdns.org                              │
│                                                      │
│  ┌───────────────┐  Docker bridge  ┌──────────────┐  │
│  │ workshop_apache│ ── /api/* ───► │ workshop_app │  │
│  │ httpd:2.4     │   (HTTP:8080)   │ Spring Boot  │  │
│  │ Puerto 80/443 │                 │(expose only) │  │
│  │ TLS offload   │                 └──────┬───────┘  │
│  └───────────────┘                        │          │
│         │ sirve                           │ JDBC     │
│   frontend/index.html            ┌────────▼───────┐  │
│                                  │workshop_postgres│  │
│                                  │ PostgreSQL 16  │  │
│                                  │ (expose only)  │  │
│                                  └────────────────┘  │
└──────────────────────────────────────────────────────┘
```

**Flujo de una petición login:**
1. Browser → HTTPS :443 → Apache (TLS termination con Let's Encrypt)
2. Apache → HTTP :8080 → Spring Boot (`workshop_app`) vía Docker bridge
3. Spring → verifica BCrypt hash en PostgreSQL
4. Spring → genera JWT firmado → Apache → Browser

---

## 📁 Estructura del Proyecto

```
secure-app-workshop/
├── Dockerfile                              # Multi-stage build (Java 21)
├── docker-compose.yml                      # Dev local
├── docker-compose.prod.yml                 # Producción AWS (TLS activo)
├── pom.xml
├── .env.example                            # Plantilla — copia como .env
├── .gitignore
├── README.md
├── frontend/
│   └── index.html                          # Cliente async HTML+JS
└── src/main/
    ├── java/com/workshop/app/
    │   ├── SecureAppApplication.java
    │   ├── config/
    │   │   ├── SecurityConfig.java         # Spring Security + JWT + CORS
    │   │   └── GlobalExceptionHandler.java
    │   ├── controller/
    │   │   ├── AuthController.java         # POST /api/auth/register  /login
    │   │   └── UserController.java         # GET  /api/user/me (protegido)
    │   ├── dto/
    │   │   ├── request/LoginRequest.java
    │   │   ├── request/RegisterRequest.java
    │   │   └── response/AuthResponse.java
    │   │   └── response/MessageResponse.java
    │   ├── model/User.java
    │   ├── repository/UserRepository.java
    │   ├── security/
    │   │   ├── JwtUtils.java
    │   │   ├── JwtAuthFilter.java
    │   │   └── UserDetailsServiceImpl.java
    │   └── service/AuthService.java        # BCrypt + lastLogin
    └── resources/
        ├── application.properties
        └── apache/
            ├── httpd.local.conf            # Dev — HTTP, proxy a Spring
            └── httpd.prod.conf             # Prod — TLS + security headers
```

---

## 🔑 Características de Seguridad

### BCrypt (cost factor 12)
Las contraseñas **nunca** se guardan en texto plano:
```java
// AuthService.java
.password(passwordEncoder.encode(req.getPassword()))  // BCrypt hash
```

### JWT Stateless
- Firmado con HMAC-SHA256
- Expira en 24 horas (configurable vía `JWT_EXPIRATION_MS`)
- Sin sesión en el servidor → escalable horizontalmente

### TLS Offloading
Apache termina HTTPS con certificados Let's Encrypt y reenvía a Spring por HTTP interno. La gestión de certificados queda fuera de la capa de aplicación.

### Cabeceras HTTP de Seguridad
```
Strict-Transport-Security: max-age=63072000; includeSubDomains; preload
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
```

---

## 🚀 Despliegue Local (sin Docker)

```bash
git clone https://github.com/TU_USUARIO/secure-app-workshop.git
cd secure-app-workshop
mvn spring-boot:run
# API en http://localhost:8080
# H2 Console: http://localhost:8080/h2-console
```

---

## ☁️ Despliegue en AWS EC2

### Paso 1 — Crear instancia EC2

1. AWS Console → **EC2 → Launch Instance**
2. AMI: **Amazon Linux 2023**, Tipo: `t3.micro`
3. **Security Group — Inbound rules:**

| Puerto | Protocolo | Fuente | Descripción |
|--------|-----------|--------|-------------|
| 22 | TCP | Tu IP | SSH |
| 80 | TCP | 0.0.0.0/0 | HTTP → redirect HTTPS |
| 443 | TCP | 0.0.0.0/0 | HTTPS |

### Paso 2 — Registrar dominio en DuckDNS

1. Ve a **[https://www.duckdns.org](https://www.duckdns.org)** → login con Google
2. Crea un subdominio, por ejemplo: `secure-workshop.duckdns.org`
3. Pega la **IP pública** de tu EC2 y haz clic en **update**
4. Verifica: `ping secure-workshop.duckdns.org`

### Paso 3 — Preparar la EC2

```bash
ssh -i "tu-clave.pem" ec2-user@secure-workshop.duckdns.org

# Instalar Docker + Docker Compose
sudo dnf update -y
sudo dnf install -y docker git unzip python3-pip
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user

sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# Reconectar para que el grupo docker tome efecto
exit
ssh -i "tu-clave.pem" ec2-user@secure-workshop.duckdns.org
```

### Paso 4 — Subir el proyecto

Desde tu PC:
```bash
scp -i "tu-clave.pem" -r ./secure-app-workshop ec2-user@secure-workshop.duckdns.org:~/
```

En la EC2:
```bash
cd secure-app-workshop
cp .env.example .env
nano .env
```

Contenido del `.env`:
```bash
DB_NAME=workshopdb
DB_USER=workshopuser
DB_PASSWORD=una_password_muy_segura

# Genera con: openssl rand -hex 64
JWT_SECRET=pega_aqui_el_resultado

JWT_EXPIRATION_MS=86400000
CORS_ALLOWED_ORIGINS=https://secure-workshop.duckdns.org
```

### Paso 5 — Certificado TLS con Let's Encrypt

```bash
# Instalar Certbot
sudo pip3 install certbot

# Obtener certificado (el puerto 80 debe estar libre)
sudo certbot certonly --standalone \
  -d secure-workshop.duckdns.org \
  --email tu@email.com \
  --agree-tos \
  --non-interactive
```

### Paso 6 — Actualizar dominio en Apache

```bash
sed -i 's/YOUR_DOMAIN/secure-workshop/g' \
  src/main/resources/apache/httpd.prod.conf
```

### Paso 7 — Levantar en producción

```bash
# Permisos a los certificados
sudo chmod 755 /etc/letsencrypt/live
sudo chmod 755 /etc/letsencrypt/archive

# Levantar todos los contenedores
docker compose -f docker-compose.prod.yml up --build -d

# Verificar
docker compose -f docker-compose.prod.yml ps
```

### Paso 8 — Verificar

```bash
# Registro
curl -X POST https://secure-workshop.duckdns.org/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"karol","email":"karol@test.com","password":"Test1234!","fullName":"Karol"}'

# Login
curl -X POST https://secure-workshop.duckdns.org/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"karol","password":"Test1234!"}'
```

---

## 📡 API Endpoints

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | No | Registrar usuario (BCrypt hash) |
| POST | `/api/auth/login` | No | Login → JWT token |
| GET | `/api/user/me` | JWT | Verificar sesión activa |

---

## 🐛 Comandos útiles

```bash
# Ver logs de todos los contenedores
docker compose -f docker-compose.prod.yml logs -f

# Logs solo de Spring
docker compose -f docker-compose.prod.yml logs -f app-service

# Reiniciar un contenedor
docker compose -f docker-compose.prod.yml restart apache

# Ver estado
docker compose -f docker-compose.prod.yml ps
```
