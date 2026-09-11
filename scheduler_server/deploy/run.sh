#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# scheduler_server 기동/종료 스크립트
#
#   ./run.sh start | stop | restart | status
#
# 환경변수는 저장소에 두지 않는다. 서버의 아래 파일에서 읽어온다.
#   /home/<user>/scheduler_server/.env  (chmod 600)
#     API_BASE_URL=http://<api 서버 주소>:8182
#     INTERNAL_API_KEY=...      # api 서버의 banking.internal.api-key 와 동일한 값
#     SNAPSHOT_CRON='0 * * * * *'   # (선택) 수집 주기 — 공백과 * 때문에 따옴표 필수
# ---------------------------------------------------------------------------
set -euo pipefail

APP_NAME="scheduler_server"
APP_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# plain jar(Main-Class 없음)는 후보에서 제외한다. 섞여 들어와도 기동이 깨지지 않도록.
JAR_PATH="$(ls -t "${APP_HOME}"/*.jar 2>/dev/null | grep -v -- '-plain\.jar$' | head -n 1 || true)"
PID_FILE="${APP_HOME}/${APP_NAME}.pid"
LOG_FILE="${APP_HOME}/${APP_NAME}.log"
ENV_FILE="${APP_HOME}/.env"
PROFILE="${SPRING_PROFILES_ACTIVE:-prod}"
HEALTH_WAIT_SECONDS="${HEALTH_WAIT_SECONDS:-40}"

current_pid() {
  [[ -f "${PID_FILE}" ]] || return 1
  local pid
  pid="$(cat "${PID_FILE}")"
  kill -0 "${pid}" 2>/dev/null || return 1
  echo "${pid}"
}

stop() {
  local pid
  if ! pid="$(current_pid)"; then
    echo "[${APP_NAME}] 실행 중인 프로세스가 없습니다."
    rm -f "${PID_FILE}"
    return 0
  fi

  echo "[${APP_NAME}] 종료 중 (pid=${pid})"
  kill "${pid}"
  for _ in $(seq 1 30); do
    kill -0 "${pid}" 2>/dev/null || break
    sleep 1
  done
  if kill -0 "${pid}" 2>/dev/null; then
    echo "[${APP_NAME}] 정상 종료에 실패해 강제 종료합니다."
    kill -9 "${pid}"
  fi
  rm -f "${PID_FILE}"
  echo "[${APP_NAME}] 종료 완료"
}

start() {
  if current_pid >/dev/null; then
    echo "[${APP_NAME}] 이미 실행 중입니다 (pid=$(cat "${PID_FILE}"))."
    return 0
  fi
  if [[ -z "${JAR_PATH}" ]]; then
    echo "[${APP_NAME}] 실행할 jar 파일을 찾을 수 없습니다: ${APP_HOME}" >&2
    exit 1
  fi
  if [[ ! -f "${ENV_FILE}" ]]; then
    echo "[${APP_NAME}] 환경변수 파일이 없습니다: ${ENV_FILE}" >&2
    exit 1
  fi

  # shellcheck disable=SC1090
  set -a && source "${ENV_FILE}" && set +a

  echo "[${APP_NAME}] 기동: ${JAR_PATH} (profile=${PROFILE})"
  nohup java -jar \
    -Duser.timezone=Asia/Seoul \
    -Dspring.profiles.active="${PROFILE}" \
    "${JAR_PATH}" >> "${LOG_FILE}" 2>&1 &
  echo $! > "${PID_FILE}"

  wait_until_up
}

wait_until_up() {
  local pid
  pid="$(cat "${PID_FILE}")"
  local port="${SERVER_PORT:-8181}"

  for _ in $(seq 1 "${HEALTH_WAIT_SECONDS}"); do
    if ! kill -0 "${pid}" 2>/dev/null; then
      echo "[${APP_NAME}] 기동 실패. 로그 마지막 50줄:" >&2
      tail -n 50 "${LOG_FILE}" >&2
      rm -f "${PID_FILE}"
      exit 1
    fi
    # 핸들러가 없어 404 가 떠도 내장 톰캣이 응답했다는 뜻이다.
    if curl -s -o /dev/null -m 2 "http://localhost:${port}/"; then
      echo "[${APP_NAME}] 기동 완료 (pid=${pid}, port=${port})"
      return 0
    fi
    sleep 1
  done

  echo "[${APP_NAME}] ${HEALTH_WAIT_SECONDS}초 안에 응답하지 않았습니다. 로그 마지막 50줄:" >&2
  tail -n 50 "${LOG_FILE}" >&2
  exit 1
}

status() {
  if current_pid >/dev/null; then
    echo "[${APP_NAME}] running (pid=$(cat "${PID_FILE}"))"
  else
    echo "[${APP_NAME}] stopped"
    exit 1
  fi
}

case "${1:-}" in
  start)   start ;;
  stop)    stop ;;
  restart) stop; start ;;
  status)  status ;;
  *)
    echo "usage: $0 {start|stop|restart|status}" >&2
    exit 2
    ;;
esac
