#!/usr/bin/env bash
set -euo pipefail
# 在 java-mosskb 所在的 Linux 主机上以 root 执行:创建宿主 Python 沙箱用户与目录。
# 宿主 runner(kb.python.runner=host)用 su 切到这个用户跑用户代码,不需要 Docker。
#
# 用法: ./setup-python-host-sandbox.sh [沙箱用户] [沙箱目录] [python 可执行文件]
# 例:   ./setup-python-host-sandbox.sh sandbox /var/lib/java-mosskb/sandbox python3
USER_NAME="${1:-sandbox}"
DIR="${2:-/var/lib/java-mosskb/sandbox}"
PYTHON="${3:-python3}"

if [ "$(id -u)" != "0" ]; then
  echo "必须以 root 执行:su 只能由 root 切换用户,非 root 部署请改用 kb.python.runner=docker。" >&2
  exit 1
fi
if [ "$USER_NAME" = "root" ] || [ "$USER_NAME" = "0" ]; then
  echo "沙箱用户不能是 root。" >&2
  exit 1
fi
if ! command -v "$PYTHON" >/dev/null 2>&1; then
  echo "找不到 $PYTHON,请先安装 Python 3 或用第三个参数指定绝对路径。" >&2
  exit 1
fi

mkdir -p "$DIR"
if id -u "$USER_NAME" >/dev/null 2>&1; then
  echo "沙箱用户已存在:$USER_NAME"
else
  useradd --system --no-create-home --home-dir "$DIR" --shell /usr/sbin/nologin "$USER_NAME" 2>/dev/null \
    || useradd --system --no-create-home --home-dir "$DIR" --shell /sbin/nologin "$USER_NAME"
  echo "已创建沙箱用户:$USER_NAME"
fi

chown "$USER_NAME" "$DIR"
chmod 0750 "$DIR"
echo "沙箱目录:$DIR (属主 $USER_NAME,权限 0750)"

echo "== 验证提权命令能切到沙箱用户执行 $PYTHON =="
PYTHON_PATH="$(command -v "$PYTHON")"
RUNUSER=""
for candidate in /usr/sbin/runuser /sbin/runuser /usr/bin/runuser /bin/runuser; do
  if [ -x "$candidate" ]; then
    RUNUSER="$candidate"
    break
  fi
done
if [ -z "$RUNUSER" ]; then
  echo "找不到 runuser(util-linux)。java-mosskb 默认用 runuser 提权:它不是 setuid 程序,argv 直接传递。" >&2
  echo "没有 runuser 时可以配置 kb.python.sandbox.su,但实测由 JVM 启动 setuid 的 su 会报 Authentication failure," >&2
  echo "默认 posix_spawn 下甚至会卡死,所以不推荐;请先安装 util-linux。" >&2
  exit 1
fi
RESULT="$("$RUNUSER" -u "$USER_NAME" -- "$PYTHON_PATH" -I -B -c "import json, os; print(json.dumps({'uid': os.getuid(), 'secret_absent': 'GITEE_API_KEY' not in os.environ}))")"
echo "$RESULT"
EXPECTED_UID="$(id -u "$USER_NAME")"
if ! printf '%s' "$RESULT" | grep -q "\"uid\": $EXPECTED_UID"; then
  echo "runuser 得到的 uid 不是 $EXPECTED_UID,请检查 runuser 与沙箱用户配置。" >&2
  exit 1
fi

cat <<EOF

沙箱就绪。在 mosskb-web/my.txt 里设置:

kb.python.runner=host
kb.python.sandbox.user=$USER_NAME
kb.python.sandbox.dir=$DIR
kb.python.sandbox.python=$PYTHON_PATH

再检查两件事:
1. java-mosskb 必须以 root 运行(提权命令需要),否则加载时会拒绝执行;
2. 沙箱用户可以读宿主上任何 others 可读的文件,请确认 mosskb-web/my.txt、mosskb-web/secrets.txt
   等含密钥的文件是 chmod 600,而不是默认的 644。
3. 宿主 runner 不提供断网、只读根文件系统和内存/CPU/pids 上限;需要这些保证时仍应使用
   kb.python.runner=docker。
EOF
