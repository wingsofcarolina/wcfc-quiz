#!/bin/bash

# Inner script that runs inside the wcfc-testing container
# This script contains all the commands that need to be executed within the container

set -e

echo -e "Starting integration test execution inside container"

# Wait for services to start
echo -e "Waiting for services to start..."
sleep 1

# Check if MongoDB is ready
echo -e "Checking MongoDB connection..."
for i in {1..30}; do
    if mongosh --eval "db.adminCommand('ping')" >/dev/null 2>&1; then
        echo -e "MongoDB is ready"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "MongoDB failed to start"
        exit 1
    fi
    sleep 2
done

# Check if WireMock is ready
echo -e "Checking WireMock connection..."
for i in {1..30}; do
    if curl -s http://localhost:8080/__admin/health >/dev/null 2>&1; then
        echo -e "WireMock is ready"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "WireMock failed to start"
        exit 1
    fi
    sleep 2
done

# Setup test data in MongoDB and WireMock
echo -e "Setting up test data..."
python3 /app/test-scripts/setup-test-data.py

# Setup WireMock stubs
echo -e "Setting up WireMock stubs..."
python3 /app/test-scripts/setup-wiremock.py

# Create necessary directories
echo -e "Creating necessary directories..."
mkdir -p /data /images /log

# Test MongoDB connection before starting app
echo -e "Testing MongoDB connection..."
mongosh --eval "db.runCommand({ping: 1})" || echo "MongoDB ping failed"

# Verify test data was inserted correctly
echo -e "Verifying test data..."
mongosh wcfc-quiz --eval "print('Recipes count: ' + db.Recipe.countDocuments({})); db.Recipe.find().forEach(printjson);"
mongosh wcfc-quiz --eval "print('Questions count: ' + db.Question.countDocuments({}));"

# Test the exact query that the application would use
echo -e "Testing recipe lookup query..."
mongosh wcfc-quiz --eval "print('Recipe lookup result:'); db.Recipe.find({alias: 'STUDENT_TEST'}).forEach(printjson);"

# Start the application in the background
echo -e "Starting WCFC Quiz application..."
java --add-opens java.base/java.lang=ALL-UNNAMED \
     --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
     --add-opens java.base/java.security=ALL-UNNAMED \
     -jar /app/wcfc-quiz.jar server /app/test-data/configuration.yml &
APP_PID=$!

# Wait for application to start
echo -e "Waiting for application to start..."
for i in {1..60}; do
    if ! kill -0 "$APP_PID" 2>/dev/null; then
      echo -e "Application has exited"
      exit 1
    fi
    if curl -s http://localhost:9314/admin/healthcheck >/dev/null 2>&1; then
        echo -e "Application is ready"
        # Test if the generate endpoint is accessible
        echo -e "Testing generate endpoint availability..."
        curl -I http://localhost:9314/generate/STUDENT_TEST 2>/dev/null | head -1 || echo "Generate endpoint test failed"
        break
    fi
    if [ $i -eq 60 ]; then
        echo -e "Application failed to start"
        exit 1
    fi
    sleep 1
done

# Activate the Playwright venv
. /usr/local/playwright/venv/bin/activate

# Run the Playwright test
echo -e "Running Playwright quiz test..."
python3 /app/test-scripts/quiz-flow-test.py

# Check test results
if [ $? -eq 0 ]; then
    echo -e "Integration test completed successfully!"
    # Kill the application process
    kill $APP_PID 2>/dev/null || true
    exit 0
else
    echo -e "Integration test failed!"
    # Kill the application process
    kill $APP_PID 2>/dev/null || true
    exit 1
fi
