# docker build --platform linux/amd64 -t prokudintema/compprehension-server-bg:0.1.0 -t prokudintema/compprehension-server-bg:latest -f server-bg.Dockerfile .
# docker push prokudintema/compprehension-server-bg
FROM eclipse-temurin:21-jre
COPY ./modules/background-server/target/background-server-*.jar app.jar
COPY ./modules/expr-domain-question-generator/target/expr-domain-question-generator-*.jar /generator/generator.jar

# Generate the runner.sh script
RUN echo '#!/bin/bash' > /generator/runner.sh && \
    echo 'java -jar /generator/generator.jar "$@"' >> /generator/runner.sh && \
    chmod +x /generator/runner.sh

# Install Python and clean up
RUN apt-get update && \
    apt-get install -y python3 python3-pip python-is-python3 && \
    pip install --no-cache-dir pycparser && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV TASK_GENERATION_GENERATOR_PATH_TO_EXECUTABLE=/generator/runner.sh

ENTRYPOINT ["java","-jar","/app.jar"]
