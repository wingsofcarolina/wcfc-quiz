#!/usr/bin/env bash
set -euo pipefail

gcloud secrets list --format="value(name)" | while read -r secret; do
  echo "Syncing secret: $secret"
  podman secret rm --ignore "$secret"
  gcloud secrets versions access latest --secret="$secret" \
    | podman secret create "$secret" -
done
