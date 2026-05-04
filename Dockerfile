# ===========================================================
#  PPM Backend — Spring Boot 3.4.5 / Java 17
# ===========================================================
#  Build : docker build -t ppm-backend .
#  Run   : docker run -p 8082:8082 --name ppm-back ppm-backend
#
#  ARCHITECTURE — multi-stage build :
#    Stage 1 (build)  : maven:3.9-eclipse-temurin-17-alpine  (~900 MB avec JDK + Maven)
#    Stage 2 (runtime): eclipse-temurin:17-jre-alpine        (~80 MB — JRE seul)
#    COPY --from=build copie uniquement le JAR compile entre les deux stages.
#    L'image finale ne contient ni Maven, ni le JDK, ni les sources — surface d'attaque reduite.
#
#  SECURITE :
#    - Utilisateur non-root (ppm:ppm) creee avant le ENTRYPOINT
#    - Aucun secret dans l'image — DB_PASSWORD et JWT_SECRET injectes a l'execution
#      via --env-file (Docker) ou secretKeyRef (Kubernetes/Helm)
#
#  FICHIER SUIVANT A LIRE : ppm-gitops/charts/ppm-backend/templates/deployment.yaml
#    → ce fichier montre comment cette image est deployee dans Kubernetes
# ===========================================================

# ---- Stage 1: Build with Maven ----
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Cache dependencies (re-downloaded only when pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and package
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Lightweight JRE runtime ----
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S ppm && adduser -S ppm -G ppm

# Create directory for uploaded project files
RUN mkdir -p /app/projects && chown -R ppm:ppm /app

# Copy built JAR from build stage
COPY --from=build /app/target/PPM_project-0.0.1-SNAPSHOT.jar app.jar

# Switch to non-root user
USER ppm

EXPOSE 8082

# ---- Non-secret runtime defaults ----
# DDL_AUTO=update is safe for development. For production, override with DDL_AUTO=validate
# and enable the prod Spring profile: SPRING_PROFILES_ACTIVE=prod
# Secrets (DB_PASSWORD, JWT_SECRET) MUST be injected at runtime via --env or --env-file.
ENV SERVER_PORT=8082 \
    FILE_UPLOAD_DIR=/app/projects \
    DDL_AUTO=update

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
  CMD wget -qO- http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
