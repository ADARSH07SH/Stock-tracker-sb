FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd -m -u 1001 appuser
COPY --from=builder /build/target/*.jar app.jar
RUN chown appuser:appuser app.jar
USER appuser
EXPOSE 8081
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=45.0","-XX:InitialRAMPercentage=25.0","-XX:+UseG1GC","-XX:+ExitOnOutOfMemoryError","-jar","app.jar"]