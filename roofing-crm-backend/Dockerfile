# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
ENV MAVEN_CONFIG=""

# Copy Maven descriptor + wrapper config first for caching
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd ./

# Fix Windows CRLF line endings and ensure executable
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Cache dependencies
RUN ./mvnw -q -DskipTests dependency:go-offline

# Build app
COPY src/ src/
RUN ./mvnw -q -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
