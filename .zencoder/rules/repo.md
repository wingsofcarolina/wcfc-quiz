---
description: Repository Information Overview
alwaysApply: true
---

# WCFC Quiz Information

## Summary
A quiz generation system for the Wings of Carolina Flying Club. The application is built using Java with Dropwizard framework and provides quiz functionality for flying club members.

## Structure
- **src/**: Main application source code
- **populate/**: Subproject for populating the database
- **integration-tests/**: Integration test scripts and data
- **.mvn/**: Maven configuration files
- **.github/**: GitHub workflows and configuration

## Output Directories

Note that these directories are in .gitignore, so you will normally be denied access to them.

- **target/**: Compiled Java classes and output from the Maven build process
- **docker/**: Dockerfile and build files, mostly copied from src/main/resources

## Language & Runtime
**Language**: Java
**Version**: Java 21
**Build System**: Maven
**Package Manager**: Maven

## Dependencies
**Main Dependencies**:
- Dropwizard 5.0.0 (core, client, assets, auth, metrics)
- MongoDB (mongo-java-driver 3.12.14, morphia 1.3.2)
- Asciidoctor (asciidoctorj 3.0.0, asciidoctorj-pdf 2.3.19)
- JRuby 10.0.2.0
- Mustache Java 0.9.14
- JWT 0.13.0
- CommonMark 0.26.0
- iText PDF 9.3.0
- Groovy 3.0.25
- FreeMarker 2.3.34

**Development Dependencies**:
- Maven plugins (shade, compiler, resources, jar, etc.)
- Prettier Maven Plugin 0.22

## Build & Installation
```bash
# Build the application
make

# Build Docker image
make build

# Run integration tests
make integration-tests
```

## Docker
**Base Image**: azul/zulu-openjdk-alpine:21-latest
**Configuration**: 
- Sets timezone to America/New\_York
- Creates directories for app, templates, and extensions
- Uses entrypoint.sh script to start the application

## Testing
**Unit Tests**: As of now there are no unit tests for this app.
**Integration Tests**: There are end-to-end integration tests in `integration-tests/`.  These use Playwright for browser automation and WireMock for mocking external APIs.
**Run Command**:
```bash
make integration-tests
```

## Deployment
This app is expected to run in Google Cloud Run.  In its production deployment, it does not have a long-running process; instead, its container is launched on demand when there is traffic.  As a result, internal maintenance tasks are scheduled opportunistically at app start.

## Subprojects

### Populate
**Purpose**: Database population utility
**Configuration File**: populate/pom.xml
**Main Class**: org.wingsofcarolina.populate.Populate
**Dependencies**: 
- wcfc-quiz (main project)
- mongo-java-driver 3.4.0
- commons-collections4 4.3
- lorem 2.1
