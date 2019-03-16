######

# Location of the MongoDB server
# If on a Docker network, just give the name of the MongoDB
# container. Otherwise give the IP:PORT for the MongoDB process.
mongodb: ${MONGODB!'mongodb'}

# Location of the template directory
templates: ${TEMPLATES!'./templates'}

# Configure ports used by DropWizard
server:
    type: simple
    connector:
        type: http
        port: ${SERVER_PORT!'9314'}
    applicationContextPath: /
    adminContextPath: /admin
    requestLog:
        appenders:
          - type: file
            currentLogFilename: log/quiz-http.log
            threshold: ALL
            archive: true
            archivedLogFilenamePattern: log/quiz-%i-%d-http.log
            maxFileSize: 500MB
            archivedFileCount: 5
            timeZone: UTC
    
# SLF4j Logging settings.
logging:
  # The default level of all loggers. Can be OFF, ERROR, WARN, INFO, DEBUG, TRACE, or ALL.
  level: ${DEFAULT_LOGLEVEL!'INFO'}

  # Logger-specific levels.
  loggers:
    "org.wingsofcarolina.quiz": ${LOGLEVEL!'INFO'}
    
  appenders:
      - type: console
        threshold: ALL
        timeZone: UTC
        target: stdout
      - type: file
        currentLogFilename: ./log/quiz.log
        threshold: ALL
        archive: true
        archivedLogFilenamePattern: ./log/quiz-%i-%d.log
        maxFileSize: 500MB
        archivedFileCount: 5
        timeZone: UTC
