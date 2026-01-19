FROM eclipse-temurin:21-jre

WORKDIR /app

# Build the jar with local Maven first, then build the image.
# Example: mvn -DskipTests package && docker build --build-arg JAR_FILE=target/xxx.jar -t howtogrow-backend .
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/app.jar

RUN mkdir -p /app/logs

EXPOSE 8080
ENV JAVA_OPTS=""
ENV LOG_DIR="/app/logs"
VOLUME ["/app/logs"]
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
