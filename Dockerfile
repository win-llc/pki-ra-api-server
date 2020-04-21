FROM openjdk:8-jdk-alpine

ENV JAVA_OPTS="-Djdk.tls.trustNameService=false -Dcom.net.ssl.enableECC=false -DtlsClientParameters.useHttpsURLConnectionDefaultHostnameVerifier=true -Djavax.net.ssl.trustStore=/ssl/trust.jks"
RUN mkdir -p /ssl
ARG JAR_FILE=target/*.jar
COPY "build/libs/PKI Registration Authority-1.0-SNAPSHOT.jar" app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar