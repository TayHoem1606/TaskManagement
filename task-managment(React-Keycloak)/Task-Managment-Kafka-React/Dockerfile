# ============================================================
# Stage 1 — BUILD
# Use eclipse-temurin which has all required tools (tar, curl, etc.)
# ============================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Install tar explicitly (just in case)
RUN apt-get update && apt-get install -y tar && rm -rf /var/lib/apt/lists/*

# Copy Maven wrapper and pom.xml first
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Fix mvnw line endings (Windows CRLF → Linux LF) and make executable
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Download all dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code and build the JAR
COPY src src
RUN ./mvnw clean package -DskipTests -B

# ============================================================
# Stage 2 — RUNTIME
# Slim JRE only — no Maven, no source code
# ============================================================
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]