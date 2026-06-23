# Task Management System — Dockerized Version

> **Spring Boot 4 · Apache Kafka 4 · MongoDB 7 · Docker**
>
> A production-style Task Management REST API built with Reactive Programming, Domain-Driven Design (DDD), and Event-Driven Architecture (EDA), fully containerized with Docker Compose.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture & Tech Stack](#2-architecture--tech-stack)
3. [Prerequisites](#3-prerequisites)
4. [Project Structure](#4-project-structure)
5. [Configuration Files Explained](#5-configuration-files-explained)
   - [Dockerfile](#51-dockerfile)
   - [docker-compose.yml](#52-docker-composeyml)
   - [application.properties](#53-applicationproperties)
   - [pom.xml](#54-pomxml)
6. [How to Build & Run (Step by Step)](#6-how-to-build--run-step-by-step)
7. [Docker Commands Reference](#7-docker-commands-reference)
8. [Viewing Logs](#8-viewing-logs)
9. [API Endpoints & Testing](#9-api-endpoints--testing)
10. [Event Flow Summary](#10-event-flow-summary)
11. [Troubleshooting](#11-troubleshooting)

---

## 1. Project Overview

This project is a **Task Management REST API** that demonstrates four advanced patterns:

| Pattern | How It Is Applied |
|---|---|
| **Reactive Programming** | Spring WebFlux + Project Reactor (`Mono`, `Flux`) — non-blocking I/O on Netty |
| **Domain-Driven Design** | Code split into Domain / Application / Infrastructure / Presentation / Shared layers |
| **Event-Driven Architecture** | HTTP writes publish Spring events → Kafka → Consumer → MongoDB (fully decoupled) |
| **Dockerization** | Three containers (App + Kafka + MongoDB) orchestrated by Docker Compose |

### How a write operation works (EDA flow)

```
HTTP Request (POST/PUT/PATCH/DELETE)
    │
    ▼
TaskController          → publishes domain event (e.g. TaskCreatedEvent)
    │
    ▼
KafkaEventHandler       → @Async @EventListener — converts to TaskEventDTO
    │
    ▼
KafkaProducerService    → kafkaTemplate.send() → Kafka topic: task-events
    │
    ▼
KafkaConsumerService    → @KafkaListener — receives and routes event
    │
    ▼
TaskServiceImpl         → validates + saves to MongoDB
    │
    ▼
MongoDB                 → task persisted
```

> **Read operations (GET) bypass Kafka entirely** — they query MongoDB directly.

---

## 2. Architecture & Tech Stack

```
┌─────────────────────────────────────────────────────────────┐
│                     Docker Network (task-network)           │
│                                                             │
│  ┌──────────────────┐   ┌──────────┐   ┌────────────────┐  │
│  │ task-management- │   │ mongodb  │   │     kafka      │  │
│  │      app         │──▶│ :27017   │   │  :9092 / :9093 │  │
│  │    :8080         │   │          │   │                │  │
│  └──────────────────┘   └──────────┘   └────────────────┘  │
│          │                                      ▲           │
│          └──────────── Kafka events ────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

| Component | Technology | Version |
|---|---|---|
| Application Framework | Spring Boot | 4.0.3 |
| Web Layer | Spring WebFlux (Netty) | 4.0.x |
| Database | MongoDB (Reactive) | 7.0 |
| Message Broker | Apache Kafka (KRaft mode) | 4.0.0 |
| Kafka Integration | spring-kafka | 4.0.x |
| Java | Eclipse Temurin JDK/JRE | 21 (LTS) |
| Build Tool | Maven Wrapper | 3.x |
| Container Runtime | Docker / Rancher Desktop | — |

---

## 3. Prerequisites

Before you begin, make sure the following are installed on your machine:

### Required

| Tool | Purpose | Download |
|---|---|---|
| **Rancher Desktop** (or Docker Desktop) | Container runtime | https://rancherdesktop.io |
| **Java 21** | Local development / IntelliJ | https://adoptium.net |
| **Maven** (or use `./mvnw`) | Build tool | bundled via Maven Wrapper |
| **Git** | Clone the repository | https://git-scm.com |

### Verify your setup

```bash
# Check Docker is running
docker --version
docker compose version

# Check Java
java -version
```

Expected output:
```
Docker version 24.x.x
Docker Compose version v2.x.x
openjdk version "21.x.x"
```

> **Note:** Rancher Desktop must be **running** before executing any `docker compose` commands.

---

## 4. Project Structure

```
SpringB-Kafka-MongoDB-Demo/
│
├── Dockerfile                          ← Multi-stage Docker build
├── docker-compose.yml                  ← Orchestrates App + Kafka + MongoDB
├── pom.xml                             ← Maven dependencies
├── mvnw / mvnw.cmd                     ← Maven wrapper scripts
│
└── src/main/
    ├── resources/
    │   └── application.properties      ← App configuration
    │
    └── java/org/example/tay/springbkafkamongodbdemo/
        │
        ├── SpringBKafkaMongoDbDemoApplication.java   ← Entry point
        │
        ├── domain/                     ← DOMAIN LAYER
        │   ├── model/
        │   │   ├── Task.java           ← Aggregate root (@Document)
        │   │   ├── TaskStatus.java     ← Enum: PENDING, IN_PROGRESS, COMPLETED
        │   │   └── TaskPriority.java   ← Enum: LOW, MEDIUM, HIGH
        │   ├── event/
        │   │   ├── TaskCreatedEvent.java
        │   │   ├── TaskUpdatedEvent.java
        │   │   └── TaskDeletedEvent.java
        │   └── repository/
        │       └── TaskRepository.java ← ReactiveMongoRepository interface
        │
        ├── application/                ← APPLICATION LAYER
        │   └── service/
        │       ├── TaskService.java    ← Interface (CQRS style)
        │       └── impl/
        │           └── TaskServiceImpl.java ← Business logic + validation
        │
        ├── infrastructure/             ← INFRASTRUCTURE LAYER
        │   ├── config/
        │   │   └── KafkaConfig.java   ← All Kafka beans (producer + consumer)
        │   └── messaging/
        │       ├── KafkaEventHandler.java   ← @EventListener → Kafka
        │       ├── KafkaProducerService.java ← kafkaTemplate.send()
        │       └── KafkaConsumerService.java ← @KafkaListener
        │
        ├── presentation/               ← PRESENTATION LAYER
        │   └── controller/
        │       └── TaskController.java ← REST endpoints
        │
        └── shared/                     ← SHARED LAYER
            ├── dto/
            │   ├── TaskRequestDTO.java
            │   ├── TaskResponseDTO.java
            │   ├── TaskEventDTO.java
            │   └── TaskStatusUpdateDTO.java
            ├── exception/
            │   ├── GlobalExceptionHandler.java
            │   ├── TaskNotFoundException.java
            │   ├── ConflictException.java
            │   └── ErrorResponse.java
            └── mapper/
                └── TaskMapper.java     ← MapStruct mapper
```

---

## 5. Configuration Files Explained

### 5.1 Dockerfile

The Dockerfile uses a **multi-stage build** to produce a small, secure runtime image.

```dockerfile
# ============================================================
# Stage 1 — BUILD
# Uses full JDK + Maven to compile and package the application
# ============================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Install tar (required by Maven wrapper)
RUN apt-get update && apt-get install -y tar && rm -rf /var/lib/apt/lists/*

# Copy pom.xml FIRST — Docker caches this layer
# Dependencies are only re-downloaded when pom.xml changes
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Fix Windows CRLF line endings → Linux LF (required on Windows hosts)
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Download all dependencies (cached layer — speeds up rebuilds)
RUN ./mvnw dependency:go-offline -B

# Copy source code and compile
COPY src src
RUN ./mvnw clean package -DskipTests -B

# ============================================================
# Stage 2 — RUNTIME
# Slim JRE only — no Maven, no JDK tools, no source code
# Result: ~250MB image vs ~450MB with full JDK
# ============================================================
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Copy ONLY the compiled JAR from Stage 1
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Key decisions explained:**

| Line | Why |
|---|---|
| `eclipse-temurin:21-jdk-jammy` | Ubuntu 22.04 LTS base with full JDK — needed to compile |
| `eclipse-temurin:21-jre-jammy` | Slim JRE only — no compiler, smaller attack surface |
| `sed -i 's/\r$//' mvnw` | Converts Windows CRLF → Linux LF. Without this, Linux throws `/bin/sh: bad interpreter` |
| `dependency:go-offline -B` | Pre-downloads all Maven deps into a cached Docker layer — subsequent builds skip this if `pom.xml` unchanged |
| `-DskipTests` | Tests should run in CI pipeline, not during Docker image build |
| `COPY --from=builder` | Only the JAR crosses from Stage 1 to Stage 2 — source code and Maven cache are discarded |

---

### 5.2 docker-compose.yml

```yaml
version: '3.8'

services:

  # ============================================================
  # Spring Boot Application
  # ============================================================
  app:
    build:
      context: SpringB-Kafka-MongoDB-Demo
      dockerfile: Dockerfile
    container_name: task-management-app
    ports:
      - "8080:8080"           # host:container
    environment:
      # CRITICAL: must be SPRING_DATA_MONGODB_* (not SPRING_MONGODB_*)
      # 'mongodb' and 'kafka' are container names — resolvable on task-network
      SPRING_DATA_MONGODB_URI: mongodb://mongodb:27017/taskDB
      SPRING_DATA_MONGODB_DATABASE: taskDB
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      mongodb:
        condition: service_started   # MongoDB starts fast, Spring handles reconnect
      kafka:
        condition: service_healthy   # Wait for Kafka healthcheck before starting app
    restart: on-failure
    networks:
      - task-network

  # ============================================================
  # MongoDB
  # ============================================================
  mongodb:
    image: mongo:7
    container_name: mongodb
    restart: always
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db   # Named volume — data survives 'docker compose down'
    networks:
      - task-network

  # ============================================================
  # Apache Kafka (KRaft mode — no ZooKeeper)
  # ============================================================
  kafka:
    image: apache/kafka:4.0.0
    container_name: kafka
    restart: always
    ports:
      - "9092:9092"
    healthcheck:
      test: [ "CMD-SHELL", "/opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 > /dev/null 2>&1" ]
      interval: 15s
      timeout: 10s
      retries: 10
      start_period: 30s
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller          # KRaft: single node as both
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092  # Must use container name
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
      CLUSTER_ID: 5L6g3nShT-eMCtK--X86sw
    networks:
      - task-network

volumes:
  mongo_data:       # Persists MongoDB data across restarts

networks:
  task-network:
    driver: bridge  # All containers on the same virtual network
```

**Key settings explained:**

| Setting | Why It Matters |
|---|---|
| `SPRING_DATA_MONGODB_URI: mongodb://mongodb:27017/taskDB` | Inside a container, `localhost` = the container itself. Use `mongodb` (container name) to reach the MongoDB container across the Docker network |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092` | Same principle — `kafka` resolves to the Kafka container's IP on `task-network` |
| `condition: service_healthy` for Kafka | Without this, the app starts before Kafka is ready, connection fails, and `restart: on-failure` loops |
| `KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092` | Kafka tells clients to connect to `kafka:9092` — must use container name, not `localhost` |
| `KAFKA_PROCESS_ROLES: broker,controller` | KRaft mode — Kafka 4.0 removed ZooKeeper. One node acts as both broker and controller |
| `CLUSTER_ID` | Fixed base64 ID for KRaft cluster. If you change Kafka image version and keep old volume data, run `docker compose down -v` first to avoid ID mismatch crash |
| `mongo_data` volume | Survives `docker compose down` but deleted by `docker compose down -v` |

---

### 5.3 application.properties

```properties
# ==============================================
# Application
# ==============================================
spring.application.name=SpringB-Kafka-MongoDB-Demo

# ==============================================
# MongoDB
# IMPORTANT: must be spring.DATA.mongodb.* not spring.mongodb.*
# When running in Docker, overridden by SPRING_DATA_MONGODB_URI env var
# ==============================================
spring.data.mongodb.uri=mongodb://localhost:27017/taskDB
spring.data.mongodb.database=taskDB

# ==============================================
# Kafka Broker
# When running in Docker, overridden by SPRING_KAFKA_BOOTSTRAP_SERVERS env var
# ==============================================
spring.kafka.bootstrap-servers=localhost:9092

# ==============================================
# Kafka Consumer
# ==============================================
spring.kafka.consumer.group-id=task-management-group

# ==============================================
# Kafka Admin — don't crash on startup if Kafka is slow to start
# ==============================================
spring.kafka.admin.fail-fast=false
spring.kafka.admin.properties.request.timeout.ms=30000

# ==============================================
# Kafka Topic (custom property — injected via @Value)
# NOTE: Do NOT use dots inside custom property keys after the prefix
# ==============================================
kafka.topic.name=task-events

# ==============================================
# Jackson
# ==============================================
spring.jackson.deserialization.fail-on-unknown-properties=false
```

**Critical notes:**

- `spring.data.mongodb.uri` — the `data` part is **mandatory**. Using `spring.mongodb.uri` is silently ignored by Spring Boot 4 and the app connects to the default `localhost:27017/test` database instead
- Serializer/deserializer config is **not** in `application.properties` — it is defined programmatically in `KafkaConfig.java` to ensure the correct `ObjectMapper` with `JavaTimeModule` is used for `LocalDate` fields

---

### 5.4 pom.xml

**Parent & Java version:**
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.3</version>
</parent>
<properties>
    <java.version>21</java.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
</properties>
```

**Key dependencies and their purpose:**

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-webflux` | Reactive REST API with Netty. Replaces Spring MVC. Provides `@RestController`, `Mono`, `Flux` |
| `spring-boot-starter-data-mongodb-reactive` | Non-blocking MongoDB. `ReactiveMongoRepository` returns `Mono<T>` / `Flux<T>` |
| `spring-kafka` | Kafka integration — `KafkaTemplate`, `@KafkaListener`, `JacksonJsonSerializer`, `JacksonJsonDeserializer` |
| `spring-boot-starter-validation` | Bean Validation (`@NotBlank`, `@NotNull`, `@Size`) on DTOs |
| `spring-boot-starter-json` | Jackson 3 JSON support — `ObjectMapper`, `@JsonFormat` |
| `spring-boot-configuration-processor` | Generates IntelliJ autocomplete metadata for `application.properties` |
| `lombok` | Removes boilerplate — `@Data`, `@Builder`, `@Slf4j`, `@RequiredArgsConstructor` |
| `mapstruct` | Compile-time DTO mapper (Task → TaskResponseDTO) |
| `mapstruct-processor` | Annotation processor that generates `TaskMapperImpl.java` at compile time |

> **Note:** `jackson-datatype-jsr310` is intentionally **excluded**. Spring Boot 4 uses Jackson 3 which has `JavaTimeModule` built-in. Including the old dependency causes a Jackson 2 vs Jackson 3 conflict.

**Annotation processor order (critical):**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <!-- Lombok MUST come before MapStruct -->
            <!-- MapStruct reads Lombok-generated getters/setters -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>1.18.42</version>
            </path>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

## 6. How to Build & Run (Step by Step)

### Step 1 — Clone the repository

```bash
git clone <your-repo-url>
cd SpringB-Kafka-MongoDB-Demo
```

### Step 2 — Make sure Rancher Desktop (or Docker Desktop) is running

Open Rancher Desktop and wait for it to show **Running** status before continuing.

### Step 3 — Build and start all containers

```bash
docker compose up -d --build
```

This single command:
1. Builds the Spring Boot application Docker image (downloads deps, compiles, packages JAR)
2. Pulls `mongo:7` and `apache/kafka:4.0.0` images if not cached
3. Creates the `task-network` bridge network
4. Starts MongoDB container
5. Starts Kafka container and waits for its healthcheck to pass
6. Starts the Spring Boot app container (only after Kafka is healthy)

> **First run takes 3–5 minutes** — Maven downloads all dependencies. Subsequent builds with no `pom.xml` changes take ~30 seconds.

### Step 4 — Verify all containers are running

```bash
docker compose ps
```

Expected output:

```
NAME                   IMAGE                  STATUS                    PORTS
task-management-app    springb-.../app        Up About a minute         0.0.0.0:8080->8080/tcp
mongodb                mongo:7                Up About a minute         0.0.0.0:27017->27017/tcp
kafka                  apache/kafka:4.0.0     Up About a minute         0.0.0.0:9092->9092/tcp
```

All three containers must show **Up**. If Kafka shows **Up (health: starting)**, wait another 30 seconds.

### Step 5 — Confirm the app started successfully

```bash
docker logs task-management-app --tail=30
```

Look for these lines — they confirm everything is connected:

```
Monitor thread successfully connected to server with description ... mongodb:27017
partitions assigned: [task-events-0]
Netty started on port 8080 (http)
Started SpringBKafkaMongoDbDemoApplication in X.XXX seconds
```

> If you do **not** see `partitions assigned`, the Kafka consumer is not running. See [Troubleshooting](#11-troubleshooting).

### Step 6 — Test the API

```bash
# Create a task
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "My first task",
    "description": "Testing the dockerized app",
    "status": "PENDING",
    "priority": "HIGH",
    "dueDate": "2026-12-01"
  }'
```

Expected response:
```
202 Accepted
Task creation event published. Task ID: TASK-XXXXXXXX
```

Wait 2 seconds (Kafka is async), then verify it was saved:

```bash
curl http://localhost:8080/api/tasks
```

Expected response:
```json
[
  {
    "id": "TASK-XXXXXXXX",
    "title": "My first task",
    "description": "Testing the dockerized app",
    "status": "PENDING",
    "priority": "HIGH",
    "dueDate": "2026-12-01",
    "createdAt": "2026-XX-XXTXX:XX:XX",
    "updatedAt": "2026-XX-XXTXX:XX:XX"
  }
]
```

---

## 7. Docker Commands Reference

### Start & Stop

```bash
# Start all containers (first time or after code changes)
docker compose up -d --build

# Start without rebuilding (when only config changed)
docker compose up -d

# Stop all containers (keeps MongoDB data)
docker compose down

# Stop all containers AND delete all data volumes
# Use when upgrading Kafka version or doing a full reset
docker compose down -v

# Restart just the app container (after code change, faster than full rebuild)
docker compose restart app
```

### Status & Inspection

```bash
# Show status of all containers
docker compose ps

# Show running containers (all Docker containers on the system)
docker ps

# Show resource usage (CPU, memory) of all containers
docker stats
```

### Rebuilding

```bash
# Full clean rebuild (wipe volumes + rebuild image)
docker compose down -v && docker compose up -d --build

# Rebuild only the app image (MongoDB and Kafka untouched)
docker compose build app && docker compose up -d
```

---

## 8. Viewing Logs

### Stream app logs in real time

```bash
docker logs task-management-app -f
```

Press `Ctrl+C` to stop following.

### Show last N lines

```bash
docker logs task-management-app --tail=50
docker logs task-management-app --tail=200
```

### Filter logs on Linux/macOS

```bash
# Show only errors and warnings
docker logs task-management-app 2>&1 | grep -E "ERROR|WARN"

# Show Kafka consumer activity
docker logs task-management-app 2>&1 | grep -E "Received|Published|saved|deleted"

# Show MongoDB connection
docker logs task-management-app 2>&1 | grep "mongodb"
```

### Filter logs on Windows PowerShell

```powershell
# Show only errors and warnings
docker logs task-management-app 2>&1 | Select-String -Pattern "ERROR|WARN"

# Show Kafka consumer activity
docker logs task-management-app 2>&1 | Select-String -Pattern "Received|Published|saved|deleted"

# Show partition assignment (confirms consumer is alive)
docker logs task-management-app 2>&1 | Select-String -Pattern "partitions assigned|task-events"

# Show MongoDB connection status
docker logs task-management-app 2>&1 | Select-String -Pattern "mongodb|MongoDB"
```

### What healthy startup logs look like

```
INFO  --- [main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data Reactive MongoDB
INFO  --- [main] .s.d.r.c.RepositoryConfigurationDelegate : Found 1 Reactive MongoDB repository interface
INFO  --- [}-mongodb:27017] org.mongodb.driver.cluster : Monitor thread successfully connected to server
INFO  --- [main] o.s.boot.reactor.netty.NettyWebServer : Netty started on port 8080 (http)
INFO  --- [main] e.t.s.SpringBKafkaMongoDbDemoApplication : Started SpringBKafkaMongoDbDemoApplication in 3.241 seconds
INFO  --- [...] o.a.k.clients.consumer.KafkaConsumer : [Consumer ...] Subscribed to topic(s): task-events
INFO  --- [...] o.a.k.c.c.internals.ConsumerCoordinator : [Consumer ...] Setting newly assigned partitions: task-events-0
```

### What a successful CREATE event looks like in the logs

```
INFO  --- [or-http-epoll-3] o.e.t.s.p.controller.TaskController      : POST /api/tasks - Publishing TaskCreatedEvent for: TASK-1001
INFO  --- [task-1]          o.e.t.s.i.m.KafkaEventHandler            : Handling TaskCreatedEvent for task: TASK-1001
INFO  --- [task-1]          o.e.t.s.i.m.KafkaProducerService         : Published Kafka Event: TASK_CREATED for TASK-1001
INFO  --- [...listener-0-C-1] o.e.t.s.i.m.KafkaConsumerService       : Received Kafka Event: TASK_CREATED for TASK-1001
INFO  --- [...listener-0-C-1] o.e.t.s.a.service.impl.TaskServiceImpl  : Processing TASK_CREATED for: TASK-1001
INFO  --- [...listener-0-C-1] o.e.t.s.a.service.impl.TaskServiceImpl  : Task saved to MongoDB: TASK-1001
```

### View Kafka, MongoDB logs

```bash
# Kafka container logs
docker logs kafka --tail=50

# MongoDB container logs
docker logs mongodb --tail=50
```

### Inspect messages inside Kafka topic

```bash
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --topic task-events \
  --bootstrap-server localhost:9092 \
  --from-beginning
```

---

## 9. API Endpoints & Testing

### Base URL

```
http://localhost:8080
```

### Endpoints

| Method | URL | Description | Response |
|---|---|---|---|
| `POST` | `/api/tasks` | Create a new task | `202 Accepted` |
| `GET` | `/api/tasks` | Get all tasks | `200 OK` + JSON array |
| `GET` | `/api/tasks/{id}` | Get task by ID | `200 OK` + JSON object |
| `PUT` | `/api/tasks/{id}` | Full update (PENDING tasks only) | `202 Accepted` |
| `PATCH` | `/api/tasks/{id}/status` | Status-only update | `202 Accepted` |
| `DELETE` | `/api/tasks/{id}` | Delete a task | `202 Accepted` |

> **Why 202 Accepted for writes?** POST/PUT/PATCH/DELETE publish events to Kafka and return immediately. The actual MongoDB write happens asynchronously. 202 = "your request was accepted for processing". Always wait 1-2 seconds before calling GET to confirm the write completed.

### Sample Requests (curl)

**Create a task:**
```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Prepare internship report",
    "description": "Complete weekly internship summary",
    "status": "PENDING",
    "priority": "HIGH",
    "dueDate": "2026-06-01"
  }'
```

**Create with a custom ID:**
```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TASK-1001",
    "title": "Setup Docker environment",
    "description": "Configure Rancher Desktop with all services",
    "status": "PENDING",
    "priority": "MEDIUM",
    "dueDate": "2026-05-15"
  }'
```

**Get all tasks:**
```bash
curl http://localhost:8080/api/tasks
```

**Get task by ID:**
```bash
curl http://localhost:8080/api/tasks/TASK-1001
```

**Full update (PUT — PENDING tasks only):**
```bash
curl -X PUT http://localhost:8080/api/tasks/TASK-1001 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Setup Docker environment",
    "description": "Updated: finalized configuration",
    "status": "PENDING",
    "priority": "HIGH",
    "dueDate": "2026-06-30"
  }'
```

**Advance status — PENDING → IN_PROGRESS:**
```bash
curl -X PATCH http://localhost:8080/api/tasks/TASK-1001/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'
```

**Advance status — IN_PROGRESS → COMPLETED:**
```bash
curl -X PATCH http://localhost:8080/api/tasks/TASK-1001/status \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}'
```

**Delete a task:**
```bash
curl -X DELETE http://localhost:8080/api/tasks/TASK-1001
```

### Windows PowerShell note

PowerShell's `curl` is an alias for `Invoke-WebRequest`. Use escaped quotes:

```powershell
curl -X POST http://localhost:8080/api/tasks `
  -H "Content-Type: application/json" `
  -d '{\"title\":\"My task\",\"description\":\"Description here\",\"status\":\"PENDING\",\"priority\":\"HIGH\",\"dueDate\":\"2026-12-01\"}'
```

### Status Transition Rules

```
PENDING ──────────▶ IN_PROGRESS ──────────▶ COMPLETED
                                                 │
         ✅ ALLOWED                    ✅ ALLOWED │ Final state
                                                 │ No changes allowed
PENDING ─────────────────────────────▶ COMPLETED
                      ❌ BLOCKED (must go through IN_PROGRESS)

IN_PROGRESS ───────────────────────▶ PENDING
                      ❌ BLOCKED (cannot go backwards)
```

---

## 10. Event Flow Summary

```
┌─────────────────────────────────────────────────────────────────────────┐
│  WRITE (POST / PUT / PATCH / DELETE)                                    │
│                                                                         │
│  TaskController                                                         │
│      │  publishEvent(TaskCreatedEvent)          ← Spring Event Bus      │
│      ▼                                                                  │
│  KafkaEventHandler  (@Async @EventListener)     ← separate thread pool  │
│      │  converts domain event → TaskEventDTO                            │
│      ▼                                                                  │
│  KafkaProducerService                                                   │
│      │  kafkaTemplate.send("task-events", taskId, eventDTO)            │
│      ▼                                                                  │
│  Kafka Broker (apache/kafka:4.0.0)  topic: task-events                  │
│      │                                                                  │
│      ▼                                                                  │
│  KafkaConsumerService  (@KafkaListener)                                 │
│      │  routes by eventType → handleCreate / handleUpdate / handleDelete│
│      ▼                                                                  │
│  TaskServiceImpl                                                        │
│      │  validates → taskRepository.save() / deleteById()               │
│      ▼                                                                  │
│  MongoDB  (mongo:7)   collection: tasks                                 │
│                                                                         │
│  HTTP client ◀── 202 Accepted ──────────────────── (immediate response) │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  READ (GET)                                                             │
│                                                                         │
│  TaskController ──▶ TaskServiceImpl ──▶ TaskRepository ──▶ MongoDB     │
│                                                                         │
│  No Kafka involved — direct MongoDB query                               │
│  HTTP client ◀── 200 OK + JSON  (immediate response)                   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 11. Troubleshooting

### Problem: `GET /api/tasks` returns `[]` after creating a task

**Cause:** Wrong MongoDB connection string — app is connecting to the default database, not `taskDB`.

**Check:** `docker logs task-management-app 2>&1 | grep -i "mongodb"`

**Fix:** In `docker-compose.yml`, ensure the env var uses `SPRING_DATA_MONGODB_URI` (with `DATA`), not `SPRING_MONGODB_URI`.

---

### Problem: POST returns 202 but task never appears in GET

**Cause:** Kafka consumer is not running or deserialization is failing silently.

**Check (Linux/macOS):**
```bash
docker logs task-management-app 2>&1 | grep "partitions assigned"
```

**Check (Windows PowerShell):**
```powershell
docker logs task-management-app 2>&1 | Select-String "partitions assigned"
```

If this line is missing, the consumer never started.

**Fix:** Ensure `KafkaConfig.java` is the **only** Kafka config file. Delete `KafkaProducerConfig.java` and `KafkaConsumerConfig.java` if they exist — having duplicate bean definitions causes the consumer factory to fail silently.

---

### Problem: App container keeps restarting

**Check:**
```bash
docker logs task-management-app --tail=50
```

**Common causes:**

| Error in logs | Fix |
|---|---|
| `Connection refused kafka:9092` | Kafka not ready yet. Wait 30s and check `docker compose ps` for Kafka health status |
| `BeanCreationException` | Duplicate bean definition in Kafka config. Remove extra config files |
| `MongoTimeoutException` | MongoDB env var wrong. Check `SPRING_DATA_MONGODB_URI` spelling |

---

### Problem: Kafka container fails to start — cluster ID mismatch

**Cause:** Changed Kafka image version but old KRaft metadata (volume) has a different cluster ID.

**Fix:**
```bash
docker compose down -v
docker compose up -d --build
```

> ⚠️ This deletes all MongoDB data as well. Only use `-v` when you intend to reset all data.

---

### Problem: `mvnw: /bin/sh: bad interpreter` during Docker build

**Cause:** `mvnw` file has Windows CRLF line endings.

**Fix:** Already handled in the Dockerfile:
```dockerfile
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
```

If you still see this, ensure you copied this line into your Dockerfile.

---

### Problem: `UNKNOWN_TOPIC_OR_PARTITION` warning in logs

**Cause:** Kafka auto-creates the `task-events` topic after a brief startup delay. This is a transient warning, not an error.

**This is normal.** The producer retries and the warning resolves within a few seconds. The setting `spring.kafka.admin.fail-fast=false` prevents this from crashing the app.

---

### Problem: Consumer receives message but task not saved — validation rejection

**Check logs for:**
```
CONSUMER ERROR - Create [TASK-XXX]: Due date cannot be in the past
CONSUMER ERROR - Create [TASK-XXX]: Task ID already exists
CONSUMER ERROR - Create [TASK-XXX]: New tasks must start with status PENDING
```

These are expected business rule rejections, not bugs. The event was delivered to Kafka successfully but TaskServiceImpl rejected it during validation.

---

## Quick Reference Card

```
# Full startup from scratch
docker compose up -d --build

# Check everything is running
docker compose ps

# Watch live logs
docker logs task-management-app -f

# Stop (keep data)
docker compose down

# Stop + wipe everything
docker compose down -v

# Quick health check
curl http://localhost:8080/api/tasks
```

---

*Built with Spring Boot 4 · Kafka 4 · MongoDB 7 · Docker*
