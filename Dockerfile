FROM openjdk:8-jdk-alpine
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom -Djdk.tls.trustNameService=false -Dcom.net.ssl.enableECC=false -DtlsClientParameters.useHttpsURLConnectionDefaultHostnameVerifier=true -Djavax.net.ssl.trustStore=/ssl/trust.jks"
VOLUME /tmp
COPY build/libs/*.jar app.jar

RUN mkdir -p /ssl
COPY build/resources/main/trust.jks /ssl/trust.jks

ENTRYPOINT ["java","$JAVA_OPTS","-jar","/app.jar"]