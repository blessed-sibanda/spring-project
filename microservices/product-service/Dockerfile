FROM adoptopenjdk:16_36-jre-hotspot as builder
WORKDIR extracted
ADD ./build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM adoptopenjdk:16_36-jre-hotspot
WORKDIR application
COPY --from=builder extracted/dependencies/ ./
RUN true
COPY --from=builder extracted/spring-boot-loader/ ./
RUN true
COPY --from=builder extracted/snapshot-dependencies/ ./
RUN true
COPY --from=builder extracted/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]