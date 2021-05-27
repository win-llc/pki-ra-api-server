FROM openjdk:14-jdk-alpine
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=dev -Djdk.tls.trustNameService=false -Dcom.net.ssl.enableECC=false -DtlsClientParameters.useHttpsURLConnectionDefaultHostnameVerifier=true -Djavax.net.ssl.trustStoreType=JKS -Djavax.net.ssl.trustStore=/ssl/trust.jks -Dloader.path=/ca-plugins/*"
VOLUME /tmp
COPY build/libs/*.jar app.jar

RUN mkdir -p /ssl
#COPY build/resources/main/trust.jks /ssl/trust.jks
#COPY build/resources/main/dogtag-ca-admin-with-chain.pfx /ssl/dogtag-ca-admin-with-chain.pfx

EXPOSE 8282

#ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar
ENTRYPOINT exec java  -cp /app.jar $JAVA_OPTS org.springframework.boot.loader.PropertiesLauncher