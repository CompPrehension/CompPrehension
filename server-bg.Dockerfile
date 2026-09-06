# docker build --platform linux/amd64 -t prokudintema/compprehension-server-bg:dev -f server-bg.Dockerfile .
# docker push prokudintema/compprehension-server-bg
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
COPY --from=builder /src/modules/background-server/target/background-server-*.jar app.jar
COPY --from=builder /src/modules/expr-domain-question-generator/target/expr-domain-question-generator-*.jar /generator/generator.jar
# COPY ./modules/background-server/target/background-server-*.jar app.jar
# COPY ./modules/expr-domain-question-generator/target/expr-domain-question-generator-*.jar /generator/generator.jar

# make folders
RUN mkdir -p /parser /generator

# download dependencies
RUN set -eux; \
    apt-get update; \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      wget ca-certificates python3 python3-pip python-is-python3; \
    \
    # download parsers/generators \
    wget -q -O /parser/expr-extractor-generator.jar \
      "https://github.com/brookite/ExpressionExtractor/releases/download/1.3.2/expr-extractor-generator.jar"; \
    wget -q -O /parser/clang-task-generator \
      "https://github.com/CompPrehension/top-learning-generator/releases/download/v0.4.0/compph-task-generator"; \
    chmod +x /parser/clang-task-generator; \
    \
    # create runner scripts \
    echo '#!/bin/bash' > /parser/runner.sh && \
      echo 'java -DLOG_LEVEL_OVERRIDE=${TASK_GENERATION_PARSER_LOG_LEVEL} -jar /parser/expr-extractor-generator.jar "$@"' >> /parser/runner.sh && \
      chmod +x /parser/runner.sh; \
    echo '#!/bin/bash' > /generator/runner.sh && \
      echo 'java -DLOG_LEVEL_OVERRIDE=${TASK_GENERATION_GENERATOR_LOG_LEVEL} -jar /generator/generator.jar "$@"' >> /generator/runner.sh && \
      chmod +x /generator/runner.sh; \
    \
    # pip depedencies (nocache) & apt clean \
    pip install --no-cache-dir pycparser --break-system-packages || true; \
    apt-get clean; rm -rf /var/lib/apt/lists/*

ENV TASK_GENERATION_PARSER_PATH_TO_EXECUTABLE=/parser/runner.sh \
    TASK_GENERATION_PARSER_LOG_LEVEL=INFO \
    TASK_GENERATION_GENERATOR_PATH_TO_EXECUTABLE=/generator/runner.sh \
    TASK_GENERATION_GENERATOR_LOG_LEVEL=INFO

ENTRYPOINT ["java","-jar","/app.jar"]
