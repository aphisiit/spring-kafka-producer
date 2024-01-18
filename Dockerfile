FROM eclipse-temurin:17.0.9_9-jdk AS builder

ADD . .

RUN ./gradlew build -x test

FROM eclipse-temurin:17.0.9_9-jre-jammy
USER root

WORKDIR /opt/app-root

COPY --from=builder build/container /opt/app-root

ENTRYPOINT [ "java", "-jar", "spring-kafka-producer.jar" ]