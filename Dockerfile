FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q package

FROM eclipse-temurin:17-jre

ARG APP_VERSION="0.0.1-SNAPSHOT"
ARG VCS_REF="unknown"
ARG BUILD_DATE="unknown"

LABEL org.opencontainers.image.title="TW Lab Contract Gate"
LABEL org.opencontainers.image.description="Spring Boot quality gate for TW Core lab Bundle validation and partner exchange contract evidence."
LABEL org.opencontainers.image.source="https://github.com/connielin07/twcore-data-quality-gate"
LABEL org.opencontainers.image.version="${APP_VERSION}"
LABEL org.opencontainers.image.revision="${VCS_REF}"
LABEL org.opencontainers.image.created="${BUILD_DATE}"

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home-dir /app app

COPY --from=build --chown=app:app /workspace/target/twcore-data-quality-gate-${APP_VERSION}.jar app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]
