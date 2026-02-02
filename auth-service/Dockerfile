FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app


COPY target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","app.jar"]
