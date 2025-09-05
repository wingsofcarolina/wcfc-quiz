APP_VERSION := $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
APP_JAR := target/wcfc-quiz-$(APP_VERSION).jar
JAVA_FILES := $(shell find src/main/java/org/wingsofcarolina -name '*.java')

$(APP_JAR): pom.xml $(JAVA_FILES)
	@mvn

.PHONY: format
format:
	@echo Formatting pom.xml files...
	@find . -name pom.xml -print0 | xargs -0 -I{} bash -c 'xmllint --format --output {} {}'
	@echo Formatting Java files...
	@mvn prettier:write -q
	@echo Formatting Java files in populate app...
	@mvn -f populate/ prettier:write -q

.PHONY: check-format
check-format:
	@find . -name pom.xml -print0 | xargs -0 -I{} bash -c 'xmllint --format {} | diff -q - {} > /dev/null'
	@mvn prettier:check -q
	@mvn -f populate/ prettier:check -q

.PHONY: clean
clean:
	@rm -rfv target/ docker/

.PHONY: distclean
distclean: clean
	@rm -rfv .mvn/ log/ data/ dynamic/ images/ tmp/ pom.xml.tag pom.xml.releaseBackup pom.xml.versionsBackup pom.xml.next release.properties dependency-reduced-pom.xml

