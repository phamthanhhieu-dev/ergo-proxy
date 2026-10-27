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
# Frontend dependencies and build tooling never enter the Java build context.
COPY --from=frontend /frontend/playback/dist/ src/main/resources/static/playback/dist/
RUN --mount=type=cache,id=ergo-proxy-maven,target=/root/.m2 ./mvnw -B -q -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre-noble AS java-runtime
FROM python:3.14-slim-trixie AS runtime
LABEL io.ergo-proxy.managed="true" io.ergo-proxy.namespace="ergo-proxy"
ENV JAVA_HOME=/opt/java/openjdk \
    PATH="/opt/java/openjdk/bin:/opt/venv/bin:$PATH" \
    PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 \
    XDG_CACHE_HOME=/tmp/cache HOME=/home/app
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates libstdc++6 zlib1g libfreetype6 libfontconfig1 \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 app && useradd --uid 10001 --gid app --create-home app
COPY --from=java-runtime /opt/java/openjdk/ /opt/java/openjdk/
COPY requirements.txt /tmp/requirements.txt
RUN python -m venv /opt/venv \
    && /opt/venv/bin/pip install --no-cache-dir --only-binary=:all: -r /tmp/requirements.txt \
    && rm /tmp/requirements.txt
WORKDIR /app
COPY --from=backend --chown=10001:10001 /build/target/ergo-proxy-1.0.0.jar app.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 CMD ["python", "-c", "import os,urllib.request; r=urllib.request.urlopen('http://127.0.0.1:'+os.getenv('SERVER_PORT','8080')+'/search/index.html',timeout=3); raise SystemExit(0 if r.status==200 else 1)"]
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]
