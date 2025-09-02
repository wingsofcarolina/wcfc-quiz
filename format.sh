#!/bin/bash

echo Formatting pom.xml files...
find . -name pom.xml -exec xmllint --format --output {} {} \;

echo Formatting Java files...
mvn prettier:write
