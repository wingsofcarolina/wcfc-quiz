#!/bin/bash

FILE=$1

if [ -z "$FILE" ]
then
  echo "Enter a file to upload"
else
  if [ -z "$SERVER" ]
  then
    export SERVER=http://localhost:9314
  fi
  echo "Uploading ${FILE} to ${SERVER}"
  curl ${SERVER}/api/upload -F "file=<${FILE};type=application/json"
fi
