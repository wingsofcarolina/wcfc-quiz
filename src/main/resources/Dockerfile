FROM azul/zulu-openjdk-alpine:8

RUN apk update && apk add --no-cache curl less tzdata

ENV TZ=America/New_York

RUN mkdir -p /opt/app/templates
RUN mkdir -p /opt/app/extensions
COPY . /opt/app/
RUN chmod +x /opt/app/entrypoint.sh

WORKDIR /opt/app
ENTRYPOINT ["/opt/app/entrypoint.sh"]
