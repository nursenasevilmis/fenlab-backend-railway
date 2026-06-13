# ⚗️ FenLab Backend

REST API backend for **FenLab** — a platform where students can discover, share, and interact with science experiments.

> Built with Kotlin & Spring Boot 3 · PostgreSQL · MinIO · Redis · Docker

---

## ✨ Features

- 🔐 JWT-based authentication & authorization (Spring Security)
- 🧪 Full experiment management (CRUD, soft delete, pagination, filtering)
- 📁 File storage with MinIO (images, videos, PDFs)
- 💬 Comment system
- ⭐ Rating system
- ❤️ Favorites
- ❓ Q&A on experiments
- 📄 PDF generation with iTextPDF
- 🔔 Notification system
- 🗄️ Database migrations with Flyway
- 📊 Actuator health & metrics endpoints
- 📖 Swagger UI (OpenAPI 3)

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| Framework | Spring Boot 3.5 |
| Security | Spring Security + JWT (JJWT) |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| Object Storage | MinIO |
| Cache | Redis |
| PDF Generation | iTextPDF 7 |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| Containerization | Docker + Docker Compose |

---

## 📂 Project Structure

```
src/main/kotlin/com/nursenasevilmis/fenlab/
│
├── config/
│   ├── SecurityConfig.kt       # Spring Security, CORS, JWT filter
│   ├── MinioConfig.kt          # MinIO client setup
│   ├── MinioProperties.kt      # MinIO config properties
│   ├── JwtProperties.kt        # JWT config properties
│   └── JacksonConfig.kt        # JSON serialization config
│
├── controller/
│   ├── AuthController.kt
│   ├── ExperimentController.kt
│   ├── CommentController.kt
│   ├── RatingController.kt
│   ├── FavoriteController.kt
│   ├── FileUploadController.kt
│   ├── PdfController.kt
│   ├── QuestionController.kt
│   ├── NotificationController.kt
│   └── UserController.kt
│
├── service/                    # Service interfaces + implementations
├── repository/                 # Spring Data JPA repositories
│
├── model/
│   ├── User.kt
│   ├── Experiment.kt
│   ├── ExperimentStep.kt
│   ├── ExperimentMaterial.kt
│   ├── ExperimentMedia.kt
│   ├── Comment.kt
│   ├── Rating.kt
│   ├── Favorite.kt
│   ├── Question.kt
│   ├── Notification.kt
│   ├── PdfDownload.kt
│   └── enums/                  # SubjectType, DifficultyLevel, EnvironmentType, etc.
│
├── dto/
│   ├── request/                # Request DTOs
│   └── response/               # Response DTOs
│
├── exception/                  # Global exception handler + custom exceptions
└── util/                       # FileUtils, SecurityUtils, SlugUtils, etc.
```

---

## 🚀 Getting Started

### Prerequisites
- Docker & Docker Compose
- JDK 17 (only if running without Docker)

### Run with Docker

1. Clone the repository:
```bash
git clone https://github.com/username/fenlab-backend.git
cd fenlab-backend
```

2. Create a `.env` file in the project root:
```env
DB_PASSWORD=your_db_password
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=your_minio_password
JWT_SECRET=your-jwt-secret-must-be-at-least-64-characters-long-xxxxxxxxxxxxxxxx
```

3. Build and start all services:
```bash
docker compose up -d --build
```

4. Verify everything is running:
```bash
docker compose ps
```

5. Access Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

---

## 🐳 Docker Services

| Container | Port | Description |
|-----------|------|-------------|
| `fenlab-backend` | `8080` | Spring Boot API |
| `fenlab-postgres` | `5433` | PostgreSQL database |
| `fenlab-minio` | `9000` / `9001` | MinIO object storage / console |
| `fenlab-redis` | `6379` | Redis cache |

> PostgreSQL and Redis are not exposed to the host by default — they are only accessible within the Docker network.

---

## 🔌 API Endpoints

### Auth
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/auth/register` | Register a new user | ❌ |
| POST | `/api/auth/login` | Login and receive JWT | ❌ |

### Experiments
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/experiments` | List all experiments (filterable, paginated) | ❌ |
| GET | `/api/experiments/{id}` | Get experiment detail | ❌ |
| POST | `/api/experiments` | Create a new experiment | ✅ |
| PUT | `/api/experiments/{id}` | Update experiment | ✅ |
| DELETE | `/api/experiments/{id}` | Soft-delete experiment | ✅ |
| GET | `/api/experiments/user/{userId}` | Get user's experiments | ❌ |
| GET | `/api/experiments/subjects` | List all subjects | ❌ |

### Comments
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/comments/experiment/{id}` | Get comments for an experiment | ❌ |
| POST | `/api/comments/experiment/{id}` | Add a comment | ✅ |
| PUT | `/api/comments/{id}` | Update a comment | ✅ |
| DELETE | `/api/comments/{id}` | Delete a comment | ✅ |

### Other Resources
| Resource | Base Path |
|----------|-----------|
| Ratings | `/api/ratings` |
| Favorites | `/api/favorites` |
| File Upload | `/api/files` |
| PDF Export | `/api/pdf` |
| Questions | `/api/questions` |
| Notifications | `/api/notifications` |
| Users | `/api/users` |

> Full interactive documentation available at `/swagger-ui.html`

---

## ⚙️ Configuration

The app uses Spring profiles. Set `SPRING_PROFILES_ACTIVE=prod` in production.

Key `application.properties` settings:

```properties
server.port=8080
spring.jpa.hibernate.ddl-auto=update
spring.flyway.enabled=true
spring.servlet.multipart.max-file-size=100MB
minio.buckets.videos=fenlab-videos
minio.buckets.images=fenlab-images
minio.buckets.pdfs=fenlab-pdfs
minio.buckets.profiles=fenlab-profiles
```

---

## 🗂️ Enums Reference

**SubjectType:** `SCIENCE`, `PHYSICS`, `CHEMISTRY`, `BIOLOGY`, `MATH`, `OTHER`

**DifficultyLevel:** `EASY`, `MEDIUM`, `HARD`

**EnvironmentType:** `HOME`, `LABORATORY`, `CLASSROOM`, `OUTDOOR`

**SortType:** `MOST_RECENT`, `MOST_POPULAR`, `HIGHEST_RATED`

---

## 🔒 Security

- Passwords are hashed with BCrypt
- All protected endpoints require a valid `Authorization: Bearer <token>` header
- JWT tokens expire after 24 hours; refresh tokens after 7 days
- CORS is configured to allow all origins by default (restrict in production)

---

## 📦 Useful Commands

```bash
# Start all services
docker compose up -d

# View logs
docker compose logs -f fenlab-backend

# Restart backend only
docker compose restart fenlab-backend

# Rebuild after code changes
docker compose up -d --build fenlab-backend

# Stop everything
docker compose down

# Stop and remove volumes (WARNING: deletes all data)
docker compose down -v
```

---

## 👩‍💻 Developer

**Nursena Sevilmiş**  
Computer Engineering — Aydın Adnan Menderes University
