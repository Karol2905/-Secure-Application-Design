# =============================================================
#  Dockerfile — secure-app-workshop
#  Multi-stage: Stage 1 compila con Maven, Stage 2 ejecuta
#  con JRE mínimo y usuario no-root.
# =============================================================

# ── Stage 1: Build ────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Primero solo el pom.xml para cachear dependencias Maven
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Luego el código fuente
COPY src ./src

# Genera target/secure-app-workshop.jar
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

# Usuario no-root — nunca ejecutar como root en un contenedor
RUN groupadd --system appgroup && \
    useradd  --system --gid appgroup appuser

WORKDIR /app

COPY --from=builder /app/target/secure-app-workshop.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

# Toda la configuración sensible se inyecta vía variables de entorno
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
