FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/vacation-tool-backend-0.0.1-SNAPSHOT.jar app.jar
ENV DB_URL=jdbc:postgresql://db:5432/vacationtool
ENV DB_USER=vacation
ENV DB_PASSWORD=vacation
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
