FROM azul/zulu-openjdk-alpine:8

RUN apk update && apk add --no-cache curl less

COPY . /opt/app/
RUN chmod +x /opt/app/entrypoint.sh

WORKDIR /opt/app
ENTRYPOINT ["/opt/app/entrypoint.sh"]
