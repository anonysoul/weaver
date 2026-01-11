FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV JAVA_OPTS=""
RUN apk add --no-cache docker-cli
VOLUME ["/app/session-logs"]
COPY backend/build/libs/*.jar /app/app.jar
COPY frontend/dist/weaver/browser /app/static
EXPOSE 8765
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
