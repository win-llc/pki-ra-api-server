FROM openjdk:8-jdk-alpine
ENV JAVA_OPTS="-Djdk.tls.trustNameService=false -Dcom.net.ssl.enableECC=false -DtlsClientParameters.useHttpsURLConnectionDefaultHostnameVerifier=true -Djavax.net.ssl.trustStore=/ssl/trust.jks"
VOLUME /tmp
COPY build/libs/*.jar app.jar

RUN mkdir -p /ssl
COPY build/resources/trust.jks /ssl/trust.jks

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]