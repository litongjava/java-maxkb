#!/usr/bin/env bash
set -euo pipefail
# 在运行 Docker 的 Linux 主机上以 root 执行。
#
# 目的:让局域网里的 java-mosskb 通过 TCP 使用本机 Docker 引擎运行隔离 Python 容器。
# 做法:用 socat 容器把 /var/run/docker.sock 转发到 TCP 端口,不修改 dockerd 配置、不重启 Docker,
#       因此不会影响正在运行的容器。明文 Docker API 等同于该主机的 root 权限,所以默认只放行一个来源 IP。
#
# 用法: ./enable-remote-docker-tcp.sh <允许访问的客户端IP> [端口]
# 例:   ./enable-remote-docker-tcp.sh 192.168.31.225 2375
ALLOW_FROM="${1:?用法: enable-remote-docker-tcp.sh <允许访问的客户端IP> [端口]}"
PORT="${2:-2375}"
NAME="docker-tcp-${PORT}"
IMAGE="${DOCKER_TCP_IMAGE:-alpine/socat}"

echo "== 1/3 启动转发容器 ${NAME} (${IMAGE}),监听 ${PORT} =="
if docker ps -a --format '{{.Names}}' | grep -qx "${NAME}"; then
  echo "容器 ${NAME} 已存在,跳过创建"
else
  docker run -d --name "${NAME}" --restart unless-stopped \
    -p "${PORT}:${PORT}" \
    -v /var/run/docker.sock:/var/run/docker.sock \
    "${IMAGE}" tcp-listen:"${PORT}",fork,reuseaddr unix-connect:/var/run/docker.sock
fi

echo "== 2/3 只放行 ${ALLOW_FROM},其余来源丢弃 =="
if iptables -C DOCKER-USER -p tcp --dport "${PORT}" ! -s "${ALLOW_FROM}" -j DROP 2>/dev/null; then
  echo "规则已存在"
else
  iptables -I DOCKER-USER -p tcp --dport "${PORT}" ! -s "${ALLOW_FROM}" -j DROP
  iptables -L DOCKER-USER -n --line-numbers | head -5
fi

echo "== 3/3 自检 =="
curl -s "http://127.0.0.1:${PORT}/_ping" && echo
docker ps --filter "name=${NAME}" --format '{{.Names}}  {{.Status}}  {{.Ports}}'
echo
echo "注意:iptables 规则与容器都不会自动跨重启保留规则本身。"
echo "     重启主机后请重新执行本脚本(脚本可重复执行);或把上面这条 iptables 规则写入开机脚本。"
echo "     明文 TCP 2375 没有认证,只要能连上就等于拿到本机 root 权限;"
echo "     更安全的替代方案是 2376 双向 TLS 或 ssh:// 隧道。"
