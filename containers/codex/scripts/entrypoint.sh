#!/usr/bin/env bash
set -euo pipefail

export NVM_DIR="${NVM_DIR:-/root/.nvm}"

if [ -s "${NVM_DIR}/nvm.sh" ]; then
  # shellcheck disable=SC1090
  . "${NVM_DIR}/nvm.sh"
  if ! nvm ls "${NODE_VERSION}" >/dev/null 2>&1; then
    nvm install "${NODE_VERSION}"
  fi
  nvm use "${NODE_VERSION}"
fi

exec "$@"
