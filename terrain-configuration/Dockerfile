FROM openjdk:17
VOLUME /tmp
ARG JAR_FILE=target/terrain-configuration-1.0.0.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
