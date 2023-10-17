# docker build --platform linux/amd64 -t prokudintema/compprehension-server-bg:0.1.0 -t prokudintema/compprehension-server-bg:latest -f server-bg.Dockerfile .
# docker push prokudintema/compprehension-server-bg
FROM eclipse-temurin:21-jre
COPY ./modules/background-server/target/background-server-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
