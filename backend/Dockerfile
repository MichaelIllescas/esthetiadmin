# Stage 1: build the jar (tests run in CI/locally, they need a database)
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# Stage 2: small runtime image (JRE only, Alpine)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S esthetiadmin && adduser -S esthetiadmin -G esthetiadmin
COPY --from=build /app/target/backend-*.jar app.jar
USER esthetiadmin

ENV TZ=America/Argentina/Buenos_Aires \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms64m -Xmx200m -XX:MaxMetaspaceSize=128m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Duser.timezone=America/Argentina/Buenos_Aires"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
