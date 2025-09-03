APP_VERSION := $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
APP_JAR := target/wcfc-quiz-$(APP_VERSION).jar
JAVA_FILES := $(shell find src/main/java/org/wingsofcarolina -name '*.java')

$(APP_JAR): pom.xml $(JAVA_FILES)
	@mvn

.PHONY: format
format:
	@echo Formatting pom.xml files...
	@find . -name pom.xml -exec xmllint --format --output {} {} \;
	@echo Formatting Java files...
	@mvn prettier:write -q
	@echo Formatting Java files in populate app...
	@cd populate && mvn prettier:write -q

.PHONY: clean
clean:
	rm -rf target/

