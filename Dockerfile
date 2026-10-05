FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/gestao-0.0.1-SNAPSHOT.jar /app/app.jar
ENV PORT=8080
VOLUME ["/app/data"]
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
