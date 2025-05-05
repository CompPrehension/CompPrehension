# mvn clean package -DskipTests
# docker build --platform linux/amd64 -t prokudintema/compprehension-server-bg:0.1.0 -t prokudintema/compprehension-server-bg:latest -f server-bg.Dockerfile .
# docker push prokudintema/compprehension-server-bg
FROM eclipse-temurin:21-jre
COPY ./modules/background-server/target/background-server-*.jar app.jar
COPY ./modules/expr-domain-question-generator/target/expr-domain-question-generator-*.jar /generator/generator.jar

# download parser for expr_domain
RUN mkdir /parser && \
    wget "https://github.com/brookite/ExpressionExtractor/releases/download/1.2.2e/expr-extractor-generator.jar" -O /parser/expr-extractor-generator.jar

# download parser for ctrlflow
RUN wget "https://github.com/CompPrehension/top-learning-generator/releases/download/v0.4.0/compph-task-generator" -O /parser/clang-task-generator && \
    chmod +x /parser/clang-task-generator

# Generate the runner.sh script for parser
RUN echo '#!/bin/bash' > /parser/runner.sh && \
    echo 'java -DLOG_LEVEL_OVERRIDE=${TASK_GENERATION_PARSER_LOG_LEVEL} -jar /parser/expr-extractor-generator.jar "$@"' >> /parser/runner.sh && \
    chmod +x /parser/runner.sh

# Generate the runner.sh script for generator
RUN echo '#!/bin/bash' > /generator/runner.sh && \
    echo 'java -DLOG_LEVEL_OVERRIDE=${TASK_GENERATION_GENERATOR_LOG_LEVEL} -jar /generator/generator.jar "$@"' >> /generator/runner.sh && \
    chmod +x /generator/runner.sh

# Install Python and clean up
RUN apt-get update && \
    apt-get install -y python3 python3-pip python-is-python3 && \
    pip install --no-cache-dir pycparser --break-system-packages && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV TASK_GENERATION_PARSER_PATH_TO_EXECUTABLE=/parser/runner.sh \
    TASK_GENERATION_PARSER_LOG_LEVEL=INFO \
    TASK_GENERATION_GENERATOR_PATH_TO_EXECUTABLE=/generator/runner.sh \
    TASK_GENERATION_GENERATOR_LOG_LEVEL=INFO

ENTRYPOINT ["java","-jar","/app.jar"]
