FROM azul/zulu-openjdk-alpine:19-latest

RUN apk update && apk add --no-cache curl less

RUN mkdir -p /opt/app/templates
RUN mkdir -p /opt/app/extensions
COPY . /opt/app/
RUN chmod +x /opt/app/entrypoint.sh

WORKDIR /opt/app
ENTRYPOINT ["/opt/app/entrypoint.sh"]
