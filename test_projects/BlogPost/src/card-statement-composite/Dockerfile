FROM java:8-jre
VOLUME /tmp
ENV environment=docker
ADD target/*.jar app.jar
ENTRYPOINT java -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Djava.security.egd=file:/dev/./urandom -Dserver.contextPath=/card-statement-composite -Dspring.profiles.active=$environment -jar /app.jar
EXPOSE 8080
