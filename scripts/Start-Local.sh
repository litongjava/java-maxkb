#!/usr/bin/env bash
#
# 在 Linux / WSL 上本地启动 Java MaxKB：先确保后端 JAR 已构建，再后台启动 Java 与前端 Vite。
#
# 用法：
#   ./scripts/Start-Local.sh                 # 用已有 JAR 启动
#   ./scripts/Start-Local.sh --build         # 先执行 Maven 打包再启动
#   ./scripts/Start-Local.sh --backend-only  # 只启动后端
#
# 环境变量：
#   BACKEND_PORT   后端端口，默认 10060
#   FRONTEND_PORT  前端端口，默认 3000
#   JAVA_BIN       java 可执行文件，默认 PATH 上的 java
#   NODE_BIN       node 可执行文件，默认 PATH 上的 node
#   MAVEN_BIN      mvn 可执行文件，默认 PATH 上的 mvn
#   FRONTEND_HOST  前端监听地址，默认 127.0.0.1；在 WSL 里要让 Windows 浏览器访问时设为 0.0.0.0
#
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WEB_ROOT="${PROJECT_ROOT}/maxkb-web"
UI_ROOT="$(cd "${PROJECT_ROOT}/.." && pwd)/java-maxkb-ui"
BACKEND_PORT="${BACKEND_PORT:-10060}"
FRONTEND_PORT="${FRONTEND_PORT:-3000}"
FRONTEND_HOST="${FRONTEND_HOST:-127.0.0.1}"
JAVA_BIN="${JAVA_BIN:-java}"
NODE_BIN="${NODE_BIN:-node}"
MAVEN_BIN="${MAVEN_BIN:-mvn}"
DO_BUILD=0
BACKEND_ONLY=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --build)
      DO_BUILD=1
      shift
      ;;
    --backend-only)
      BACKEND_ONLY=1
      shift
      ;;
    *)
      echo "未知参数：$1" >&2
      exit 1
      ;;
  esac
done

port_listening() {
  local port="$1"
  if command -v ss >/dev/null 2>&1; then
    ss -ltn "sport = :${port}" 2>/dev/null | tail -n +2 | grep -q .
  else
    netstat -ltn 2>/dev/null | grep -q ":${port} "
  fi
}

[[ -f "${WEB_ROOT}/my.txt" ]] || { echo "缺少 ${WEB_ROOT}/my.txt" >&2; exit 1; }
[[ -f "${WEB_ROOT}/secrets.txt" ]] || { echo "缺少 ${WEB_ROOT}/secrets.txt" >&2; exit 1; }

if [[ "${DO_BUILD}" -eq 1 ]]; then
  ( cd "${PROJECT_ROOT}" && "${MAVEN_BIN}" clean -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -Dgpg.skip=true -Pproduction package )
fi

JAR="$(ls -t "${WEB_ROOT}"/target/maxkb-web-*.jar 2>/dev/null | grep -vE -- '-(sources|javadoc)\.jar$' | head -n 1 || true)"
[[ -n "${JAR}" ]] || { echo "找不到后端 JAR，请先执行：./scripts/Start-Local.sh --build" >&2; exit 1; }

mkdir -p "${WEB_ROOT}/logs"

if port_listening "${BACKEND_PORT}"; then
  echo "端口 ${BACKEND_PORT} 已在监听，复用已有后端进程。"
else
  ( cd "${WEB_ROOT}" && nohup "${JAVA_BIN}" -jar "${JAR}" --server.port="${BACKEND_PORT}" \
      > "${WEB_ROOT}/logs/startup.log" 2> "${WEB_ROOT}/logs/startup-error.log" & echo $! > "${WEB_ROOT}/logs/backend.pid" )
fi

if [[ "${BACKEND_ONLY}" -eq 0 ]]; then
  if [[ ! -f "${UI_ROOT}/node_modules/vite/bin/vite.js" ]]; then
    echo "前端依赖未安装，请在 java-maxkb-ui 目录执行 npm install。" >&2
    exit 1
  fi
  mkdir -p "${UI_ROOT}/.local"
  if port_listening "${FRONTEND_PORT}"; then
    echo "端口 ${FRONTEND_PORT} 已在监听，复用已有前端进程。"
  else
    ( cd "${UI_ROOT}" && VITE_PROXY_TARGET="http://127.0.0.1:${BACKEND_PORT}" \
        nohup "${NODE_BIN}" node_modules/vite/bin/vite.js --host "${FRONTEND_HOST}" --port "${FRONTEND_PORT}" \
        > "${UI_ROOT}/.local/vite.log" 2> "${UI_ROOT}/.local/vite-error.log" & echo $! > "${UI_ROOT}/.local/frontend.pid" )
  fi
fi

echo "UI: http://localhost:${FRONTEND_PORT}/ui/  Backend: http://localhost:${BACKEND_PORT}"
echo "后端日志：maxkb-web/logs/startup.log，前端日志：java-maxkb-ui/.local/vite.log"
