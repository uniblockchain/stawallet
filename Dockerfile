# We select the base image from. Locally available or from https://hub.docker.com/
FROM openjdk:8-jre-alpine

# Install some basic tools
RUN apk --update --no-cache add bash curl

# We define the user we will use in this instance to prevent using root that even in a container, can be a security risk.
ENV APPLICATION_USER ktor

# Then we add the user, create the ./stawallet folder and give permissions to our user.
RUN adduser -D -g '' $APPLICATION_USER
RUN mkdir ./stawallet
RUN chown -R $APPLICATION_USER ./stawallet

# Marks this container to use the specified $APPLICATION_USER
USER $APPLICATION_USER

# We copy the FAT Jar we built into the /stawallet folder and sets that folder as the working directory.
COPY --chown=ktor:ktor ./build/libs/stawallet.jar ./stawallet/stawallet.jar
COPY --chown=ktor:ktor ./build/install/stawallet/bin/* ./stawallet/bin/
COPY --chown=ktor:ktor ./build/install/stawallet/lib/* ./stawallet/lib/
WORKDIR ./stawallet

# We define a volume and put the entire project on it
# TODO: This could be a security risk to put poject files inside a docker volume. In production just put logs here.
#VOLUME ["/stawallet"]

EXPOSE 8080

#CMD ["./stawallet/bin/stawallet", "database", "init"]
#CMD ["./stawallet/bin/stawallet", "database", "populate"]
#CMD ["./stawallet/bin/stawallet", "serve"]
# TODO: Set proper memory and cpu limit in java command
# We launch java to execute the jar, with good defauls intended for containers.
#CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:InitialRAMFraction=2", "-XX:MinRAMFraction=2", "-XX:MaxRAMFraction=2", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "/stawallet/stawallet.jar"]

COPY docker-entrypoint.sh /docker-entrypoint.sh
ENTRYPOINT [ "/docker-entrypoint.sh" ]

HEALTHCHECK --interval=15s --timeout=15s --retries=15 CMD curl -f http://127.0.0.1:8080/

