FROM adoptopenjdk:16-jre-hotspot
WORKDIR application
ARG JAR_FILE=build/libs/mailgroup.jar
COPY ${JAR_FILE} application.jar
CMD ["java", "-jar", "application.jar", "/application/data/queue.sq3", "/application/data/configuration.yml"]