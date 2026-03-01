
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy only pom.xml first to cache dependencies
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Now copy source code
COPY src ./src

# Build the application (without offline mode since go-offline doesn't cache everything)
RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java","-jar","app.jar"]
