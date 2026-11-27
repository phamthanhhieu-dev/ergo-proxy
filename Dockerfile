# syntax=docker/dockerfile:1.7
FROM node:24-bookworm-slim AS frontend
WORKDIR /frontend
COPY src/main/resources/static/package*.json ./
RUN --mount=type=cache,id=ergo-proxy-npm,target=/root/.npm npm ci --no-audit --no-fund
COPY src/main/resources/static/ ./
RUN npm run build

FROM eclipse-temurin:25-jdk-noble AS backend
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,id=ergo-proxy-maven,target=/root/.m2 ./mvnw -B -q -Dmaven.test.skip=true dependency:go-offline
COPY src/ src/
COPY --from=frontend /frontend/playback/dist/ src/main/resources/static/playback/dist/
RUN --mount=type=cache,id=ergo-proxy-maven,target=/root/.m2 ./mvnw -B -q -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre-noble AS runtime
LABEL io.ergo-proxy.managed="true" \
      io.ergo-proxy.namespace="ergo-proxy"
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+ExitOnOutOfMemoryError" \
    JAVA_OPTS="" \
    HOME=/home/app
RUN groupadd --gid 10001 app \
    && useradd --uid 10001 --gid app --create-home app

WORKDIR /app
COPY --from=backend --chown=10001:10001 /build/target/ergo-proxy-1.0.0.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java -Dfile.encoding=UTF-8 $JAVA_OPTS -jar /app/app.jar"]
