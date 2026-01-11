#!/usr/bin/env bash
set -euo pipefail

export NVM_DIR="${NVM_DIR:-/root/.nvm}"

if [ -s "${NVM_DIR}/nvm.sh" ]; then
  echo "[codex] 已加载 nvm 环境，准备检查 Node.js"
  # shellcheck disable=SC1090
  . "${NVM_DIR}/nvm.sh"
  if ! command -v nvm >/dev/null 2>&1; then
    echo "[codex] nvm 未正确加载，跳过 Node.js 与 codex 安装"
    exec "$@"
  fi
  set +u
  if [ -n "${NODE_VERSION:-}" ]; then
    if ! nvm ls "${NODE_VERSION}" >/dev/null 2>&1; then
      echo "[codex] 未检测到 Node.js ${NODE_VERSION}，开始安装"
      if ! nvm install "${NODE_VERSION}"; then
        echo "[codex] Node.js ${NODE_VERSION} 安装失败，继续启动"
      fi
    fi
    echo "[codex] 使用 Node.js ${NODE_VERSION}"
    if ! nvm use "${NODE_VERSION}"; then
      echo "[codex] Node.js ${NODE_VERSION} 切换失败，继续启动"
    fi
  else
    echo "[codex] 未指定 NODE_VERSION，安装并使用最新 LTS"
    if ! nvm install --lts; then
      echo "[codex] Node.js LTS 安装失败，继续启动"
    fi
    if ! nvm use --lts; then
      echo "[codex] Node.js LTS 切换失败，继续启动"
    fi
  fi
  set -u

  if command -v npm >/dev/null 2>&1; then
    if ! npm list -g @openai/codex >/dev/null 2>&1; then
      echo "[codex] 未检测到 @openai/codex，开始全局安装"
      if ! npm install -g @openai/codex; then
        echo "[codex] codex 安装失败，继续启动"
      fi
    else
      echo "[codex] 已检测到 @openai/codex"
    fi
  else
    echo "[codex] 未检测到 npm，跳过 codex 安装"
  fi
else
  echo "[codex] 未找到 nvm，跳过 Node.js 与 codex 安装"
fi

exec "$@"
