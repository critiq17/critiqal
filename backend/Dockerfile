# syntax=docker/dockerfile:1.7

# ── Stage 1: build the Quarkus fast-jar with Maven ──────────────────────────
# Maven dependency layer is cached via BuildKit cache mount so subsequent
# builds skip re-downloading the entire dependency tree when pom.xml is
# unchanged. Source-only edits rebuild only the package step.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy wrapper + pom first so the dependency layer can be cached independently
# of source changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q package -DskipTests

# ── Stage 2: minimal runtime ────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /build/target/quarkus-app/lib/      /app/lib/
COPY --from=build /build/target/quarkus-app/*.jar     /app/
COPY --from=build /build/target/quarkus-app/app/      /app/app/
COPY --from=build /build/target/quarkus-app/quarkus/  /app/quarkus/

EXPOSE 8082

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0"

ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]
