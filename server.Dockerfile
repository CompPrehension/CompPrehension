# docker build --platform linux/amd64 -t prokudintema/compprehension-server:0.1.0 -t prokudintema/compprehension-server:latest -f server.Dockerfile .
# docker push prokudintema/compprehension-server
FROM eclipse-temurin:21-jre-alpine
COPY ./modules/server/target/server-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
