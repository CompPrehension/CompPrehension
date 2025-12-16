# docker build --platform linux/amd64 -t prokudintema/compprehension-server:dev -f server.Dockerfile .
# docker push prokudintema/compprehension-server
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /src/modules/server/target/server-*.jar app.jar
#COPY ./modules/server/target/server-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
