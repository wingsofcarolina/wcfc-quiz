
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

JAR_FILE="${PROJECT_ROOT}/$(cd $PROJECT_ROOT && make -s app-jar)"
if [ ! -f "$JAR_FILE" ]; then
    echo -e "JAR file not found at $JAR_FILE"
    echo -e "Please build the application first (e.g., using the Makefile)"
    exit 1
fi

if [[ -z "$@" ]]; then
  CMD=/app/test-scripts/run-tests.sh
else
  CMD="$@"
fi

echo -e "Running integration test in container..."
export WCFC_JWT_SECRET=$(openssl rand -base64 36)
podman run -it --rm -p 9314:9314 \
    -v "$JAR_FILE:/app/wcfc-quiz.jar" \
    -v "$SCRIPT_DIR/test-data:/app/test-data" \
    -v "$SCRIPT_DIR/test-scripts:/app/test-scripts" \
    -v "$PROJECT_ROOT/src/main/resources/templates:/app/templates" \
    -v "$PROJECT_ROOT/src/main/resources/assets:/app/assets" \
    -e WCFC_JWT_SECRET \
    ghcr.io/wingsofcarolina/wcfc-integration-testing:latest\
    bash -c "$CMD"

