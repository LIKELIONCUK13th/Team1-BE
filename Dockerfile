FROM gradle:8.8-jdk17-jammy AS builder
WORKDIR /app

COPY build.gradle settings.gradle /app/
COPY gradlew /app/
COPY gradle /app/gradle
COPY .gradle /app/.gradle

COPY src /app/src

RUN chmod +x ./gradlew

RUN ./gradlew bootJar -x test --build-cache --no-daemon --refresh-dependencies

FROM openjdk:17-jdk-slim-buster
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]