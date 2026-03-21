FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/osys-auth-server-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "app.jar"]
