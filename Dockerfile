# ─────────────────────────────────────────────────────────────────────────────
# PathBoot PI – Backend Dockerfile
# Multi-stage build: compile with Maven, run with lean JRE image
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom first so dependency layer is cached unless pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy source and build (skip tests – tests run in CI, not Docker build)
COPY src src
RUN mvn package -DskipTests -B -q

# ── Stage 2: Run ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# data/ is mounted from the host via docker-compose volumes –
# the directory must exist inside the container so SQLite can write to it.
RUN mkdir -p /app/data

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

