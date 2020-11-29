FROM gradle:jdk14 AS builder

COPY . /src
WORKDIR /src

RUN useradd -u 1106 domestia
RUN gradle shadowJar


FROM openjdk:14-slim

COPY --from=builder /src/build/libs/domestia-bridge-1.0-SNAPSHOT-all.jar /app/domestia-bridge.jar
COPY --from=builder /etc/passwd /etc/passwd
COPY ./entrypoint.sh /app/entrypoint.sh

USER domestia
WORKDIR /app

ENTRYPOINT ["./entrypoint.sh"]
