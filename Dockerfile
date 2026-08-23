FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /src
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S flowforge && adduser -S flowforge -G flowforge
COPY --from=build /src/target/flowforge.jar app.jar
USER flowforge
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
