FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY . .
RUN if [ ! -f pom.xml ]; then echo "pom.xml not found; backend source not initialized" >&2; exit 1; fi
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar /app/app.jar
RUN mkdir -p /app/logs
EXPOSE 8080
ENV JAVA_OPTS=""
ENV LOG_DIR="/app/logs"
VOLUME ["/app/logs"]
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
