# Task Management System

**Spring Boot 4 · Kafka · MongoDB · React 19 · Keycloak**

A full-stack internship assignment: a reactive Task Management application with event-driven architecture, JWT-based authentication, and role/ownership-based access control.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Repository Structure](#3-repository-structure)
4. [Prerequisites](#4-prerequisites)
5. [Running with Docker (recommended)](#5-running-with-docker-recommended)
6. [Local Development (without Docker)](#6-local-development-without-docker)
7. [Demo Accounts](#7-demo-accounts)
8. [Using the Application](#8-using-the-application)
9. [REST API Reference](#9-rest-api-reference)
10. [Architecture Overview](#10-architecture-overview)
11. [Running the Tests](#11-running-the-tests)
12. [Configuration Reference](#12-configuration-reference)
13. [Known Issues & Notes](#13-known-issues--notes)
14. [Quick Start Cheatsheet](#14-quick-start-cheatsheet)

---

## 1. Project Overview

This is a full-stack Task Management application built with a reactive Spring Boot backend, Apache Kafka event streaming, MongoDB persistence, Keycloak identity management, and a React 19 single-page frontend. All services are containerised and orchestrated with Docker Compose.

**Key design principles:**

- **Event-Carried State Transfer via Kafka** — every write operation publishes an event; the Kafka consumer persists state to MongoDB asynchronously.
- **Reactive throughout** — WebFlux + Reactor on the backend; no blocking threads on the request path.
- **JWT-based security** — Keycloak issues tokens; the backend validates them with Spring OAuth2 Resource Server.
- **Fine-grained ownership** — ADMIN can modify any task; USER can only modify tasks they created.

---

## 2. Technology Stack

### Backend

| Technology | Version / Details |
|---|---|
| Spring Boot | 4.0.3 |
| Java | 21 (eclipse-temurin:21) |
| Spring WebFlux | Reactive HTTP server (Netty) |
| Spring Data MongoDB | Reactive (ReactiveMongoRepository) |
| Spring Security | OAuth2 Resource Server + JWT |
| Apache Kafka | 4.0.0 (KRaft mode — no Zookeeper) |
| Keycloak | 26.1.0 (SSO + JWT issuer) |
| MapStruct | 1.5.5.Final (DTO ↔ Entity mapping) |
| Lombok | 1.18.42 |
| Maven | 3.9.14 (via `./mvnw` wrapper) |

### Frontend

| Technology | Version / Details |
|---|---|
| React | 19.2.x |
| Vite | 8.x (build tool & dev server) |
| React Router | v7 (client-side routing) |
| @react-keycloak/web | 3.4.0 |
| keycloak-js | 26.2.x |
| Vitest | 4.x (unit tests) |
| @testing-library/react | 16.x |
| Nginx | Alpine (production static server) |

### Infrastructure

| Service | Details |
|---|---|
| MongoDB | mongo:7, port 27017, volume `mongo_data` |
| Apache Kafka | apache/kafka:4.0.0, KRaft mode, port 9092 |
| Keycloak | quay.io/keycloak/keycloak:26.1.0, port 8180 |
| Kafka UI | provectuslabs/kafka-ui, port 8090 |
| Docker network | `task-network` (bridge) |

---

## 3. Repository Structure

### Backend (`Task-Managment-Kafka-React/`)

```
Task-Managment-Kafka-React/
├── src/main/java/org/example/tay/taskmanagmentkafkareact/
│   ├── TaskManagmentKafkaReactApplication.java   ← entry point
│   ├── application/service/
│   │   ├── TaskService.java                      ← interface
│   │   └── impl/TaskServiceImpl.java             ← business logic
│   ├── domain/
│   │   ├── model/     Task, TaskStatus, TaskPriority, TaskEventType
│   │   ├── event/     TaskCreatedEvent, TaskUpdatedEvent, TaskDeletedEvent
│   │   └── repository/  TaskRepository, TaskRepositoryImpl
│   ├── infrastructure/
│   │   ├── config/    KafkaConfig, SecurityConfig, KeycloakJwtAuthConverter
│   │   └── messaging/ KafkaProducerService, KafkaConsumerService, KafkaEventHandler
│   ├── presentation/controller/TaskController.java
│   └── shared/
│       ├── dto/       TaskRequestDTO, TaskResponseDTO, TaskEventDTO, TaskFilterDTO
│       ├── exception/ GlobalExceptionHandler, ConflictException, TaskNotFoundException, KafkaPublishException
│       └── mapper/    TaskMapper.java (MapStruct)
├── src/main/resources/application.properties
├── src/test/java/.../presentation/controller/TaskControllerTest.java
├── keycloak/internship-task-realm.json           ← realm auto-import
├── docker-compose.yml
└── Dockerfile
```

### Frontend (`task-frontend/`)

```
task-frontend/
├── src/
│   ├── index.jsx           ← app entry, ReactKeycloakProvider
│   ├── App.jsx             ← routing (BrowserRouter + Routes)
│   ├── keycloak.js         ← singleton Keycloak instance
│   ├── index.css           ← global CSS variables & shared classes
│   ├── hooks/useAuth.js    ← thin wrapper around useKeycloak()
│   ├── services/taskService.js  ← fetch wrappers for all API calls
│   ├── components/
│   │   ├── Navbar.jsx   Badges.jsx   Button.jsx
│   │   ├── PrivateRoute.jsx   TaskFilterBar.jsx   TaskForm.jsx
│   │   └── *.css        (component-scoped stylesheets)
│   ├── pages/
│   │   ├── LoginPage.jsx      TaskListPage.jsx   TaskDetailPage.jsx
│   │   ├── CreateTaskPage.jsx UpdateTaskPage.jsx UnauthorizedPage.jsx
│   │   └── *.css
│   └── test/               (Vitest test files)
├── index.html
├── vite.config.js          (also contains Vitest config)
├── setup.js                (test setup: CSS mocks, keycloak mock)
├── .env                    (VITE_* env vars for local dev)
├── nginx.conf              (production Nginx config)
└── Dockerfile
```

---

## 4. Prerequisites

### For Docker (recommended)

- Docker Desktop 4.x or Docker Engine 24+ with the Compose plugin
- Minimum **6 GB RAM** allocated to Docker (Kafka + MongoDB + Keycloak each use ~512 MB–1 GB)

### For local development (optional)

- Java 21 JDK (e.g. eclipse-temurin or OpenJDK)
- Maven 3.9+ (or use the bundled `./mvnw` wrapper — no install needed)
- Node.js 20+ and npm
- A running MongoDB instance on `localhost:27017`
- A running Kafka broker on `localhost:9092`
- A running Keycloak server on `localhost:8180` with the realm imported

---

## 5. Running with Docker (recommended)

All six services start with a single command. The realm JSON is automatically imported into Keycloak on first start.

### Step 1 — Clone and start

```bash
# From the directory containing docker-compose.yml
cd Task-Managment-Kafka-React

# Build images and start all services in the background
docker compose up -d --build
```

> First startup takes 2–4 minutes: Maven downloads dependencies and Keycloak initialises the realm.

### Step 2 — Verify all services are healthy

```bash
docker compose ps
```

All six containers should show `running`. Kafka shows `healthy` once the health-check script passes (allow ~30 seconds after start).

### Step 3 — Open the application

| Service | URL |
|---|---|
| React Frontend | http://localhost:3000 |
| Spring Boot API | http://localhost:8080 |
| Keycloak Admin | http://localhost:8180 (admin / admin) |
| Kafka UI | http://localhost:8090 |
| MongoDB | mongodb://localhost:27017/taskDB |

### Stopping the application

```bash
# Stop containers but keep data (volumes persist)
docker compose down

# Stop containers AND delete all data
docker compose down -v
```

---

## 6. Local Development (without Docker)

### Step 1 — Start the infrastructure

Even for local dev, Docker is the easiest way to run MongoDB, Kafka, and Keycloak:

```bash
docker compose up -d mongodb kafka keycloak kafka-ui
```

### Step 2 — Import the Keycloak realm

The realm is imported automatically when Keycloak starts via `--import-realm`. If you need to import it manually:

1. Go to http://localhost:8180 → log in as `admin / admin`
2. Create realm → Import → upload `keycloak/internship-task-realm.json`

### Step 3 — Run the Spring Boot backend

```bash
cd Task-Managment-Kafka-React
./mvnw spring-boot:run
```

`application.properties` already points to `localhost:9092` (Kafka), `localhost:27017` (MongoDB), and `localhost:8180` (Keycloak) — no changes needed.

### Step 4 — Run the React frontend

```bash
cd task-frontend
npm install
npm run dev
```

The Vite dev server starts on http://localhost:3000. The `.env` file already contains the correct `VITE_*` variables.

---

## 7. Demo Accounts

Three users are pre-created in the `internship-task-realm`:

| Username | Password | Role & Permissions |
|---|---|---|
| `admin1` | `admin123` | **ADMIN** — full access: create, view, update, delete any task |
| `user1` | `user123` | **USER** — create & view all tasks; update/delete only own tasks |
| `user2` | `user123` | **USER** — same as user1; useful to test ownership restrictions |

> Access tokens expire after **300 seconds** (5 minutes). The frontend calls `keycloak.updateToken()` before every API request to silently refresh the token.

---

## 8. Using the Application

### 8.1 Login

Open http://localhost:3000. The Login Page shows the demo account credentials. Click **Login** — you are redirected to Keycloak. Enter credentials and you are sent back to the Task List.

### 8.2 Task List

The Task List shows all tasks in a sortable table with:

- **Column sorting** — click any column header to toggle ascending/descending order.
- **Filter bar** — filter by status, priority, title text, creator username, due date range, overdue toggle, and My Tasks toggle.
- **Overdue highlight** — tasks past their due date (and not COMPLETED) are highlighted in orange with a ⚠ warning icon.
- **Action buttons** — View is available for all tasks. Edit, Advance Status, and Delete appear only for the task owner or an ADMIN.
- **+ New Task** button in the top-right opens the Create Task form.

### 8.3 Create a Task

1. Click **+ New Task**.
2. Fill in Title (max 50 chars), Description (max 100 chars), Priority, and Due Date.
3. Status is locked to **PENDING** on creation.
4. Submit returns **202 Accepted**. The task is saved asynchronously via Kafka within ~1–2 seconds.

### 8.4 View Task Detail

Click any task ID or the **View** button to open the detail page, which shows all fields including timestamps and creator information.

### 8.5 Edit a Task

Click **Edit Task** (visible only to the owner or ADMIN). Full edits are allowed while the task is PENDING. Once IN_PROGRESS, only status can be changed via the Advance Status button.

### 8.6 Advance Status

Status transitions must follow this sequence:

```
PENDING → IN_PROGRESS → COMPLETED
```

- Skipping from PENDING directly to COMPLETED is rejected with **409 Conflict**.
- COMPLETED tasks are fully locked — no edits, no status changes. Only deletion is allowed.

### 8.7 Delete a Task

Click **Delete Task** and confirm the dialog. The owner or any ADMIN can delete a task regardless of its status (including COMPLETED).

### 8.8 Filters & Sorting

The collapsible filter bar supports:

| Filter | Behaviour |
|---|---|
| Title search | Case-insensitive regex against the title field |
| Status / Priority | Exact match dropdowns |
| Created by search | Case-insensitive regex against the username field |
| Due After / Due Before | ISO date range pickers |
| Overdue Only | `dueDate < today` AND `status ≠ COMPLETED` |
| My Tasks | Shows only tasks created by the current user (overrides Created By search) |
| Sort By / Direction | Any field, ascending or descending |
| Clear All | Resets every filter at once |

---

## 9. REST API Reference

All endpoints require a valid **Bearer JWT** in the `Authorization` header. Base URL: `http://localhost:8080`.

| Method + Path | Description | Auth |
|---|---|---|
| `GET /api/tasks` | List all tasks. Accepts filter/sort query params. | ADMIN or USER |
| `GET /api/tasks/{id}` | Fetch a single task by ID. | ADMIN or USER |
| `POST /api/tasks` | Create a task. Returns **202 Accepted** (async). | ADMIN or USER |
| `PUT /api/tasks/{id}` | Full update. Owner or ADMIN only. Returns **202**. | ADMIN or USER |
| `PATCH /api/tasks/{id}/status` | Status-only update. Returns **202**. | ADMIN or USER |
| `DELETE /api/tasks/{id}` | Delete a task. Owner or ADMIN only. Returns **202**. | ADMIN or USER |

### Filter query parameters (`GET /api/tasks`)

| Parameter | Description |
|---|---|
| `status` | `PENDING` \| `IN_PROGRESS` \| `COMPLETED` |
| `priority` | `LOW` \| `MEDIUM` \| `HIGH` |
| `titleSearch` | Case-insensitive regex on `title` |
| `createdBy` | Case-insensitive regex on `createdBy` |
| `dueAfter` | ISO date `yyyy-MM-dd` — inclusive lower bound |
| `dueBefore` | ISO date `yyyy-MM-dd` — inclusive upper bound |
| `overdueOnly` | `true` — `dueDate < today` AND `status ≠ COMPLETED` |
| `myTasksOnly` | `true` — `createdBy == current user` (from JWT) |
| `sortBy` | `title` \| `status` \| `priority` \| `dueDate` \| `createdBy` \| `createdAt` |
| `sortDir` | `asc` or `desc` (default: `desc`) |

### Task object (response)

```json
{
  "id":          "TASK-A1B2C3D4",
  "title":       "My task",
  "description": "Details",
  "status":      "PENDING",
  "priority":    "HIGH",
  "dueDate":     "2027-12-31",
  "createdAt":   "2026-04-20T14:00:00",
  "updatedAt":   "2026-04-20T14:00:00",
  "createdBy":   "user1",
  "updatedBy":   "user1"
}
```

### Error response format

```json
{
  "timestamp": "2026-04-20T14:00:00",
  "status":    409,
  "error":     "Conflict",
  "message":   "COMPLETED tasks are locked.",
  "path":      "/api/tasks/TASK-A1B2C3D4"
}
```

---

## 10. Architecture Overview

### Write operation flow

```
HTTP Request (JWT)
       │
TaskController
       ├─ taskService.generateTaskId()
       ├─ taskService.getTaskById()
       ├─ taskService.validateOwnership()   ← checks JWT roles, 403 if denied
       └─ taskService.validateUpdate()      ← status transition rules
       │
ApplicationEventPublisher
       │  TaskCreatedEvent / TaskUpdatedEvent / TaskDeletedEvent
       │
KafkaEventHandler  ──→  converts domain event to TaskEventDTO
       │
KafkaProducerService  ──→  publishes to "task-events" Kafka topic
       │
KafkaConsumerService  ──→  receives event (.block() for failure propagation)
       │
TaskServiceImpl.handleCreate/Update/Delete()  ──→  validates & saves to MongoDB
       │
HTTP Response: 202 Accepted
```

### Security model

- JWT tokens are issued by Keycloak and contain `realm_access.roles` (`ADMIN` or `USER`).
- `KeycloakJwtAuthConverter` maps realm roles to Spring Security `GrantedAuthority` (`ROLE_ADMIN`, `ROLE_USER`).
- `SecurityConfig` requires any authenticated role for all `/api/tasks/**` endpoints.
- Ownership validation runs in `TaskServiceImpl.validateOwnership()` via `ReactiveSecurityContextHolder` — called **before** the Kafka publish while the JWT context is still active.
- ADMIN bypasses ownership checks. USER gets 403 when attempting to modify another user's task.

### Kafka error handling

- **Producer:** `ENABLE_IDEMPOTENCE=true`, retries=10, `acks=all`.
- **Consumer:** `DefaultErrorHandler` with `FixedBackOff(2000ms, 3 attempts)`.
- Failed messages are routed to the `task-events.DLT` dead-letter topic after 3 retries.
- The consumer uses `.block()` so exceptions propagate back to Kafka for retry / DLT routing.

---

## 11. Running the Tests

### Backend (JUnit 5 + Mockito)

```bash
cd Task-Managment-Kafka-React
./mvnw test
```

Required test dependencies in `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-test</artifactId>
    <scope>test</scope>
</dependency>
```

| Test Class | What it covers |
|---|---|
| `SecurityConfigTest` | 401 for all endpoints without token; malformed token; actuator health public |
| `TaskControllerTest` | ADMIN/USER access, 401 unauthenticated, 403 ownership, POST validation (400) |
| `TaskServiceImplTest` | validateOwnership for ADMIN/USER/denied; handleCreate validations; status transitions; idempotent delete |

> **Important:** `TaskControllerTest` must live in the package `...presentation.controller`, **not** in the root package. Placing it in the root package triggers a full application context scan (MongoDB is bootstrapped), causing a `ReactiveMongoTemplate` missing-bean error.

### Frontend (Vitest + React Testing Library)

```bash
cd task-frontend
npx vitest run        # single pass
npm run test          # watch mode (add "test": "vitest" to package.json scripts)
```

The Vitest config is in `vite.config.js`. The setup file is `setup.js` (mocks CSS imports and the `keycloak.js` singleton).

| Test File | What it covers |
|---|---|
| `useAuth.test.js` | All `useAuth()` return values, login, logout |
| `PrivateRoute.test.jsx` | Loading spinner, login redirect, children rendered when authenticated |
| `Navbar.test.jsx` | ADMIN/USER badges, username display, logout click, hidden when uninitialised |
| `TaskForm.test.jsx` | All validation rules, character counters, loading state, `initialValues`, create mode |
| `TaskDetailPage.test.jsx` | Button visibility by role/ownership/status, loading state, error state |
| `LoginAndListPage.test.jsx` | LoginPage states; TaskListPage Edit/Delete/View visibility, empty/error states |

**Mocking pattern:** Tests that render components using `useAuth()` mock `@react-keycloak/web` at the library level (not `useAuth` directly) to avoid module-ID mismatch. Service mocks use explicit `vi.mock()` factory functions with `vi.fn()` to ensure `.mockResolvedValue()` is available.

---

## 12. Configuration Reference

### `application.properties` (local dev defaults)

| Property | Default Value |
|---|---|
| `spring.mongodb.uri` | `mongodb://localhost:27017/taskDB` |
| `spring.kafka.bootstrap-servers` | `localhost:9092` |
| `spring.kafka.consumer.group-id` | `task-management-group` |
| `kafka.topic.name` | `task-events` |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `http://localhost:8180/realms/internship-task-realm` |
| `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | `http://localhost:8180/realms/.../openid-connect/certs` |
| `cors.allowed-origins` | `http://localhost:3000` |

### Docker Compose environment overrides (app service)

| Env Variable | Docker Value |
|---|---|
| `SPRING_MONGODB_URI` | `mongodb://mongodb:27017/taskDB` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` | `http://keycloak:8180/realms/.../certs` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` |

### Frontend `.env` variables

| Variable | Value |
|---|---|
| `VITE_KEYCLOAK_URL` | `http://localhost:8180` |
| `VITE_KEYCLOAK_REALM` | `internship-task-realm` |
| `VITE_KEYCLOAK_CLIENT_ID` | `task-frontend` |
| `VITE_API_BASE_URL` | `http://localhost:8080` |

In Docker, these are passed as build `ARG`s so Vite embeds them into the JS bundle at build time.

---

## 13. Known Issues & Notes

- **202 vs 201 on POST:** `TaskController.createTask()` returns `202 Accepted` (Kafka async). The frontend and tests expect this. Do not change to 201 without updating assertions.
- **Keycloak startup time:** The app container will restart a few times until Keycloak is fully up on a cold start. This is expected — the app recovers automatically within 1–2 minutes.
- **Token expiry:** Tokens expire after 5 minutes. The frontend calls `updateToken(30)` before every API call. If you see 401 errors in the browser, refresh the page to re-authenticate.
- **Text filter debounce:** The Title Search and Created By inputs fire an API call on every keystroke. A 400 ms debounce is recommended but not yet implemented.
- **Kafka at-least-once delivery:** In the rare case of a duplicate message, `handleCreate()` catches the duplicate ID and rejects with 409 — no data corruption occurs.

---

## 14. Quick Start Cheatsheet

| Task | Command |
|---|---|
| Start everything (Docker) | `docker compose up -d --build` |
| Stop everything | `docker compose down` |
| Wipe all data | `docker compose down -v` |
| View backend logs | `docker compose logs -f app` |
| Run backend tests | `cd Task-Managment-Kafka-React && ./mvnw test` |
| Run frontend tests | `cd task-frontend && npx vitest run` |
| Start frontend dev server | `cd task-frontend && npm run dev` |
| Start backend dev server | `cd Task-Managment-Kafka-React && ./mvnw spring-boot:run` |
| Open Kafka UI | http://localhost:8090 |
| Open Keycloak Admin | http://localhost:8180 (admin / admin) |
| Open React app | http://localhost:3000 |
