#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing required command: $1" >&2
    exit 1
  fi
}

require_cmd curl
require_cmd python3
require_cmd rg

json_get() {
  local path="$1"
  python3 -c '
import json,sys
path=sys.argv[1]
obj=json.loads(sys.stdin.read())
cur=obj
for part in path.split("."):
  if part=="":
    continue
  if isinstance(cur, dict) and part in cur:
    cur=cur[part]
  else:
    cur=None
    break
if cur is None:
  print("", end="")
elif isinstance(cur,(dict,list)):
  print(json.dumps(cur, ensure_ascii=False))
else:
  print(cur)
' "$path"
}

curl_json() {
  # args: method url [json-body] [header...]
  local method="$1"
  local url="$2"
  shift 2
  local tmp
  tmp="$(mktemp)"
  local status
  if [ "$#" -gt 0 ] && [[ "${1:-}" == "{"* ]]; then
    local body="$1"
    shift 1
    status="$(curl -sS -X "$method" "$url" -H "Content-Type: application/json" "$@" -d "$body" -o "$tmp" -w "%{http_code}")"
  else
    status="$(curl -sS -X "$method" "$url" "$@" -o "$tmp" -w "%{http_code}")"
  fi
  if [ "$status" -lt 200 ] || [ "$status" -ge 300 ]; then
    echo "HTTP ${status} for ${method} ${url}" >&2
    cat "$tmp" >&2 || true
    rm -f "$tmp"
    exit 1
  fi
  cat "$tmp"
  rm -f "$tmp"
}

curl_json_allow_fail() {
  # args: method url [json-body] [header...]
  local method="$1"
  local url="$2"
  shift 2
  local tmp
  tmp="$(mktemp)"
  local status
  if [ "$#" -gt 0 ] && [[ "${1:-}" == "{"* ]]; then
    local body="$1"
    shift 1
    status="$(curl -sS -X "$method" "$url" -H "Content-Type: application/json" "$@" -d "$body" -o "$tmp" -w "%{http_code}")"
  else
    status="$(curl -sS -X "$method" "$url" "$@" -o "$tmp" -w "%{http_code}")"
  fi
  cat "$tmp"
  rm -f "$tmp"
  echo
  echo "__HTTP_STATUS__=${status}"
}

assert_api_ok() {
  local resp="$1"
  local code
  code="$(printf "%s" "$resp" | json_get code)"
  if [ "$code" != "OK" ]; then
    echo "expected ApiResponse.code=OK, got: $code" >&2
    echo "$resp" >&2
    exit 1
  fi
}

assert_api_code() {
  local resp="$1"
  local expected="$2"
  local code
  code="$(printf "%s" "$resp" | json_get code)"
  if [ "$code" != "$expected" ]; then
    echo "expected ApiResponse.code=$expected, got: $code" >&2
    echo "$resp" >&2
    exit 1
  fi
}

echo "== Health =="
curl -fsS "${BASE_URL}/actuator/health" | cat
echo

echo "== CORS Preflight =="
PRE=$(curl -sS -i -X OPTIONS "${BASE_URL}/api/v1/miniprogram/me" \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET")
echo "$PRE" | rg -qi "access-control-allow-origin" || {
  echo "CORS preflight missing Access-Control-Allow-Origin" >&2
  exit 1
}

echo "== Admin login (admin/admin) =="
ADMIN_LOGIN="$(curl_json POST "${BASE_URL}/api/v1/admin/auth/login" '{"username":"admin","password":"admin"}')"
assert_api_ok "$ADMIN_LOGIN"
ADMIN_TOKEN="$(printf "%s" "$ADMIN_LOGIN" | json_get data.token)"
if [ -z "${ADMIN_TOKEN}" ]; then
  echo "failed to parse admin token" >&2
  exit 1
fi

AUTHZ_ADMIN=(-H "Authorization: Bearer ${ADMIN_TOKEN}")

echo "== Admin me/rbac =="
assert_api_ok "$(curl_json GET "${BASE_URL}/api/v1/admin/auth/me" "${AUTHZ_ADMIN[@]}")"
assert_api_ok "$(curl_json GET "${BASE_URL}/api/v1/admin/rbac/permissions" "${AUTHZ_ADMIN[@]}")"
assert_api_ok "$(curl_json GET "${BASE_URL}/api/v1/admin/rbac/roles" "${AUTHZ_ADMIN[@]}")"
assert_api_ok "$(curl_json GET "${BASE_URL}/api/v1/admin/rbac/admin-users" "${AUTHZ_ADMIN[@]}")"

SUFFIX="$(python3 - <<'PY'
import secrets, time
print(f"{int(time.time())}_{secrets.token_hex(4)}")
PY
)"

# ---------- Test age year (question pool is matched by integer years) ----------
AGE_YEAR="${SELFTEST_AGE_YEAR:-3}"
if ! [[ "$AGE_YEAR" =~ ^[0-9]+$ ]]; then
  echo "SELFTEST_AGE_YEAR must be an integer, got: $AGE_YEAR" >&2
  exit 1
fi
if [ "$AGE_YEAR" -lt 0 ] || [ "$AGE_YEAR" -gt 18 ]; then
  echo "SELFTEST_AGE_YEAR must be in 0..18, got: $AGE_YEAR" >&2
  exit 1
fi

# ---------- Seed base data via Admin APIs ----------
DIM_CODE="${SELFTEST_DIM_CODE:-EMOTION_MANAGEMENT}"
PLAN_NAME="SELFTEST_${SUFFIX}_月卡"
QUOTE_CONTENT="SELFTEST_${SUFFIX}: keep going"

echo "== Admin dimensions (readonly) =="
DIM_LIST="$(curl_json GET "${BASE_URL}/api/v1/admin/dimensions" "${AUTHZ_ADMIN[@]}")"
assert_api_ok "$DIM_LIST"
python3 - <<'PY' "$DIM_LIST" "$DIM_CODE"
import json,sys
resp=json.loads(sys.argv[1]); code=sys.argv[2]
items=resp.get("data") or []
if not any(d.get("code")==code for d in items):
  raise SystemExit(f"dimension code not found: {code}")
PY

echo "== Admin create plan/quote =="
curl_json POST "${BASE_URL}/api/v1/admin/plans" \
  "{\"name\":\"${PLAN_NAME}\",\"days\":30,\"priceCent\":990,\"status\":1}" \
  "${AUTHZ_ADMIN[@]}" >/dev/null
curl_json POST "${BASE_URL}/api/v1/admin/quotes" \
  "{\"content\":\"${QUOTE_CONTENT}\",\"status\":1}" \
  "${AUTHZ_ADMIN[@]}" >/dev/null

PLAN_LIST="$(curl_json GET "${BASE_URL}/api/v1/admin/plans" "${AUTHZ_ADMIN[@]}")"
assert_api_ok "$PLAN_LIST"
PLAN_ID="$(python3 - <<'PY' "$PLAN_LIST" "$PLAN_NAME"
import json,sys
resp=json.loads(sys.argv[1]); name=sys.argv[2]
for p in resp.get("data",[]) or []:
  if p.get("name")==name and p.get("status")==1:
    print(p.get("planId")); sys.exit(0)
print("")
PY
)"
[ -n "$PLAN_ID" ]

QUOTE_LIST="$(curl_json GET "${BASE_URL}/api/v1/admin/quotes" "${AUTHZ_ADMIN[@]}")"
assert_api_ok "$QUOTE_LIST"
QUOTE_ID="$(python3 - <<'PY' "$QUOTE_LIST" "$QUOTE_CONTENT"
import json,sys
resp=json.loads(sys.argv[1]); content=sys.argv[2]
for q in resp.get("data",[]) or []:
  if q.get("content")==content and q.get("status")==1:
    print(q.get("id")); sys.exit(0)
print("")
PY
)"
[ -n "$QUOTE_ID" ]

echo "== Admin update plan/quote =="
curl_json PUT "${BASE_URL}/api/v1/admin/plans/${PLAN_ID}" \
  "{\"name\":\"${PLAN_NAME}\",\"days\":30,\"priceCent\":990,\"status\":1}" \
  "${AUTHZ_ADMIN[@]}" >/dev/null
curl_json PUT "${BASE_URL}/api/v1/admin/quotes/${QUOTE_ID}" \
  "{\"content\":\"${QUOTE_CONTENT} (updated)\",\"status\":1}" \
  "${AUTHZ_ADMIN[@]}" >/dev/null

# ---------- Question pool (5 questions) ----------
echo "== Admin create question pool (5) =="
QUESTION_IDS=()
for i in 1 2 3 4 5; do
  Q_CONTENT="SELFTEST_${SUFFIX}_Q${i}: 今天状态如何？"
  Q_CREATE="$(curl_json POST "${BASE_URL}/api/v1/admin/questions" \
    "{\"minAge\":${AGE_YEAR},\"maxAge\":${AGE_YEAR},\"questionType\":\"SINGLE\",\"content\":\"${Q_CONTENT}\",\"status\":1,\"options\":[{\"content\":\"挺好\",\"suggestFlag\":0,\"improvementTip\":null,\"sortNo\":1,\"dimensionScores\":[{\"dimensionCode\":\"${DIM_CODE}\",\"score\":2}]},{\"content\":\"需要调整\",\"suggestFlag\":1,\"improvementTip\":\"试试深呼吸\",\"sortNo\":2,\"dimensionScores\":[{\"dimensionCode\":\"${DIM_CODE}\",\"score\":1}]}]}" \
    "${AUTHZ_ADMIN[@]}")"
  assert_api_ok "$Q_CREATE"
  Q_ID="$(printf "%s" "$Q_CREATE" | json_get data)"
  [ -n "$Q_ID" ]
  QUESTION_IDS+=("$Q_ID")
done

Q_LIST="$(curl_json GET "${BASE_URL}/api/v1/admin/questions?ageYear=${AGE_YEAR}&page=1&pageSize=20" "${AUTHZ_ADMIN[@]}")"
assert_api_ok "$Q_LIST"

FIRST_Q_ID="${QUESTION_IDS[0]}"
Q_DETAIL="$(curl_json GET "${BASE_URL}/api/v1/admin/questions/${FIRST_Q_ID}" "${AUTHZ_ADMIN[@]}")"
assert_api_ok "$Q_DETAIL"

echo "== Admin update one question =="
curl_json PUT "${BASE_URL}/api/v1/admin/questions/${FIRST_Q_ID}" \
  "{\"minAge\":${AGE_YEAR},\"maxAge\":${AGE_YEAR},\"questionType\":\"SINGLE\",\"content\":\"SELFTEST_${SUFFIX}_Q1: 今天状态如何？(updated)\",\"status\":1,\"options\":[{\"content\":\"挺好\",\"suggestFlag\":0,\"improvementTip\":null,\"sortNo\":1,\"dimensionScores\":[{\"dimensionCode\":\"${DIM_CODE}\",\"score\":2}]},{\"content\":\"需要调整\",\"suggestFlag\":1,\"improvementTip\":\"试试深呼吸\",\"sortNo\":2,\"dimensionScores\":[{\"dimensionCode\":\"${DIM_CODE}\",\"score\":1}]}]}" \
  "${AUTHZ_ADMIN[@]}" >/dev/null

# ---------- Import Excel endpoint ----------
IMPORT_FILE_AND_Q="$(
  SELFTEST_AGE_YEAR="${AGE_YEAR}" python3 - <<'PY' "${DIM_CODE}"
import os,sys,tempfile
from openpyxl import Workbook

dim_code=sys.argv[1]
wb=Workbook()
ws=wb.active
headers=[
  "min_age","max_age","question_content","question_type",
  "option_content","suggest_flag","improvement_tip","sort_no",
  "dimension_code","dimension_score",
]
for idx, h in enumerate(headers, start=1):
  ws.cell(row=1, column=idx, value=h)
q="SELFTEST_IMPORT_"+os.urandom(4).hex()+"：导入题目"
age_year=int(os.environ.get("SELFTEST_AGE_YEAR","3"))
ws.append([age_year,age_year,q,"SINGLE","选项A",0,"",1,dim_code,"2"])
ws.append([age_year,age_year,q,"SINGLE","选项B",1,"可改进",2,dim_code,"1"])
out=os.path.join(tempfile.gettempdir(), f"selftest_import_{os.getpid()}.xlsx")
wb.save(out)
print(out)
print(q)
PY
)"
IMPORT_FILE="$(echo "$IMPORT_FILE_AND_Q" | head -n 1)"
IMPORT_Q_CONTENT="$(echo "$IMPORT_FILE_AND_Q" | tail -n 1)"

echo "== Admin import-excel =="
IMPORT_RESP="$(curl -fsS "${BASE_URL}/api/v1/admin/questions/import-excel" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -F "file=@${IMPORT_FILE}")"
assert_api_ok "$IMPORT_RESP"
TOTAL="$(printf "%s" "$IMPORT_RESP" | json_get data.total)"
if [ "${TOTAL:-0}" -le 0 ]; then
  echo "import total should be > 0" >&2
  echo "$IMPORT_RESP" >&2
  exit 1
fi

IMPORTED_LIST="$(curl_json GET "${BASE_URL}/api/v1/admin/questions?ageYear=${AGE_YEAR}&page=1&pageSize=200" "${AUTHZ_ADMIN[@]}")"
assert_api_ok "$IMPORTED_LIST"
IMPORTED_Q_ID="$(python3 - <<'PY' "$IMPORTED_LIST" "$IMPORT_Q_CONTENT"
import json,sys
resp=json.loads(sys.argv[1]); content=sys.argv[2]
items=((resp.get("data") or {}).get("items") or [])
for it in items:
  if it.get("content")==content and it.get("status")==1:
    print(it.get("questionId")); sys.exit(0)
print("")
PY
)"
[ -n "$IMPORTED_Q_ID" ]

# ---------- Miniprogram flow ----------
echo "== Miniprogram login (mock) =="
MP_LOGIN="$(curl_json POST "${BASE_URL}/api/v1/miniprogram/auth/wechat-login" "{\"code\":\"mock:selftest_${SUFFIX}\"}")"
assert_api_ok "$MP_LOGIN"
MP_TOKEN="$(printf "%s" "$MP_LOGIN" | json_get data.token)"
[ -n "$MP_TOKEN" ]
AUTHZ_MP=(-H "Authorization: Bearer ${MP_TOKEN}")

assert_api_ok "$(curl_json GET "${BASE_URL}/api/v1/miniprogram/me" "${AUTHZ_MP[@]}")"

echo "== Miniprogram child CRUD =="
CHILD_BIRTH_DATE="$(python3 - <<'PY' "$AGE_YEAR"
from datetime import datetime
import sys
from zoneinfo import ZoneInfo
age_year=int(sys.argv[1])
today=datetime.now(ZoneInfo("Asia/Shanghai")).date()
try:
  d=today.replace(year=today.year-age_year)
except ValueError:
  # 处理 2/29 等闰日场景
  d=today.replace(year=today.year-age_year, day=28)
print(d.isoformat())
PY
)"
CHILD_CREATE="$(curl_json POST "${BASE_URL}/api/v1/miniprogram/children" \
  "{\"nickname\":\"SELFTEST_CHILD\",\"gender\":1,\"birthDate\":\"${CHILD_BIRTH_DATE}\"}" \
  "${AUTHZ_MP[@]}")"
assert_api_ok "$CHILD_CREATE"
CHILD_ID="$(printf "%s" "$CHILD_CREATE" | json_get data.childId)"
[ -n "$CHILD_ID" ]
assert_api_ok "$(curl_json GET "${BASE_URL}/api/v1/miniprogram/children" "${AUTHZ_MP[@]}")"
assert_api_ok "$(curl_json PUT "${BASE_URL}/api/v1/miniprogram/children/${CHILD_ID}" \
  "{\"nickname\":\"SELFTEST_CHILD_UPDATED\",\"gender\":1,\"birthDate\":\"${CHILD_BIRTH_DATE}\"}" \
  "${AUTHZ_MP[@]}")"

echo "== Daily assessment begin/replace/submit =="
BEGIN="$(curl_json POST "${BASE_URL}/api/v1/miniprogram/assessments/daily/begin" "{\"childId\":${CHILD_ID}}" "${AUTHZ_MP[@]}")"
assert_api_ok "$BEGIN"
SESSION_ID="$(printf "%s" "$BEGIN" | json_get data.sessionId)"
[ -n "$SESSION_ID" ]

REPLACE="$(curl_json POST "${BASE_URL}/api/v1/miniprogram/assessments/daily/sessions/${SESSION_ID}/replace" \
  "{\"childId\":${CHILD_ID},\"displayOrder\":1}" \
  "${AUTHZ_MP[@]}")"
assert_api_ok "$REPLACE"

SUBMIT_BODY="$(python3 - <<'PY' "$BEGIN" "$REPLACE" "$CHILD_ID"
import json,sys
begin=json.loads(sys.argv[1])
replace=json.loads(sys.argv[2])
child_id=int(sys.argv[3])

items=(begin.get("data") or {}).get("items") or []
new_item=(replace.get("data") or {}).get("newItem") or {}
display_order=(replace.get("data") or {}).get("displayOrder") or 0

for i,it in enumerate(items):
  if int(it.get("displayOrder",0))==int(display_order):
    items[i]=new_item

answers=[]
for it in items:
  opt=(it.get("options") or [])[0]
  answers.append({"questionId": it["questionId"], "optionIds": [opt["optionId"]]})

print(json.dumps({"childId": child_id, "answers": answers}, ensure_ascii=False))
PY
)"
SUBMIT="$(curl_json POST "${BASE_URL}/api/v1/miniprogram/assessments/daily/sessions/${SESSION_ID}/submit" \
  "$SUBMIT_BODY" \
  "${AUTHZ_MP[@]}")"
assert_api_ok "$SUBMIT"
ASSESSMENT_ID="$(printf "%s" "$SUBMIT" | json_get data.assessmentId)"
[ -n "$ASSESSMENT_ID" ]

echo "== Daily assessment begin again should fail =="
BEGIN2="$(curl_json_allow_fail POST "${BASE_URL}/api/v1/miniprogram/assessments/daily/begin" "{\"childId\":${CHILD_ID}}" "${AUTHZ_MP[@]}")"
BEGIN2_BODY="$(echo "$BEGIN2" | sed -n '1,/__HTTP_STATUS__/p' | sed '$d')"
BEGIN2_STATUS="$(echo "$BEGIN2" | sed -n 's/__HTTP_STATUS__=//p')"
if [ "$BEGIN2_STATUS" != "400" ]; then
  echo "expected HTTP 400, got: $BEGIN2_STATUS" >&2
  echo "$BEGIN2_BODY" >&2
  exit 1
fi
BEGIN2_CODE="$(printf "%s" "$BEGIN2_BODY" | json_get code)"
if [ "$BEGIN2_CODE" != "DAILY_ASSESSMENT_ALREADY_SUBMITTED" ] && [ "$BEGIN2_CODE" != "FREE_TRIAL_ALREADY_USED" ]; then
  echo "expected code DAILY_ASSESSMENT_ALREADY_SUBMITTED or FREE_TRIAL_ALREADY_USED, got: $BEGIN2_CODE" >&2
  echo "$BEGIN2_BODY" >&2
  exit 1
fi

echo "== Report/quotes/subscriptions =="
assert_api_ok "$(curl_json GET "${BASE_URL}/api/v1/miniprogram/quotes/random" "${AUTHZ_MP[@]}")"

FROM_TO="$(python3 - <<'PY'
from datetime import datetime,timedelta
from zoneinfo import ZoneInfo
end=datetime.now(ZoneInfo("Asia/Shanghai")).date(); start=end-timedelta(days=7)
print(start.isoformat(), end.isoformat())
PY
)"
FROM="$(echo "$FROM_TO" | awk '{print $1}')"
TO="$(echo "$FROM_TO" | awk '{print $2}')"
REPORT="$(curl_json GET "${BASE_URL}/api/v1/miniprogram/reports/growth?childId=${CHILD_ID}&from=${FROM}&to=${TO}" "${AUTHZ_MP[@]}")"
assert_api_ok "$REPORT"

PLANS="$(curl_json GET "${BASE_URL}/api/v1/miniprogram/subscriptions/plans" "${AUTHZ_MP[@]}")"
assert_api_ok "$PLANS"

ORDER="$(curl_json POST "${BASE_URL}/api/v1/miniprogram/subscriptions/orders" "{\"planId\":${PLAN_ID}}" "${AUTHZ_MP[@]}")"
assert_api_ok "$ORDER"
ORDER_NO="$(printf "%s" "$ORDER" | json_get data.orderNo)"
[ -n "$ORDER_NO" ]

echo "== Pay notify (mock) -> subscription granted =="
PAY_NOTIFY_BODY="$(python3 - <<'PY' "$ORDER_NO"
import json,sys
order_no=sys.argv[1]
print(json.dumps({
  "id":"evt_selftest_"+order_no,
  "event_type":"TRANSACTION.SUCCESS",
  "resource_type":"mock",
  "summary":"selftest",
  "out_trade_no": order_no,
  "transaction_id": "4200000000000000",
  "trade_state": "SUCCESS",
  "amount": {"total": 990, "currency": "CNY"},
  "success_time": "2025-12-28T00:00:00+08:00"
}, ensure_ascii=False))
PY
)"
ACK="$(curl_json POST "${BASE_URL}/api/v1/pay/wechat/notify" "$PAY_NOTIFY_BODY")"
ACK_CODE="$(printf "%s" "$ACK" | json_get code)"
if [ "$ACK_CODE" != "SUCCESS" ]; then
  echo "expected pay notify ack code=SUCCESS, got: $ACK_CODE" >&2
  echo "$ACK" >&2
  exit 1
fi

ME_AFTER_PAY="$(curl_json GET "${BASE_URL}/api/v1/miniprogram/me" "${AUTHZ_MP[@]}")"
assert_api_ok "$ME_AFTER_PAY"
SUB_END="$(printf "%s" "$ME_AFTER_PAY" | json_get data.user.subscriptionEndAt)"
[ -n "$SUB_END" ]

echo "== AI summary (real) =="
AI_SUMMARY="$(curl_json POST "${BASE_URL}/api/v1/miniprogram/assessments/daily/${ASSESSMENT_ID}/ai-summary" "${AUTHZ_MP[@]}")"
assert_api_ok "$AI_SUMMARY"
AI_CONTENT="$(printf "%s" "$AI_SUMMARY" | json_get data.content)"
[ -n "$AI_CONTENT" ]

echo "== AI chat (real stream SSE) =="
SESSION="$(curl_json POST "${BASE_URL}/api/v1/miniprogram/ai/chat/sessions" "{\"childId\":${CHILD_ID}}" "${AUTHZ_MP[@]}")"
assert_api_ok "$SESSION"
SESSION_ID="$(printf "%s" "$SESSION" | json_get data.sessionId)"
[ -n "$SESSION_ID" ]

MSG="$(curl_json POST "${BASE_URL}/api/v1/miniprogram/ai/chat/sessions/${SESSION_ID}/messages" "{\"content\":\"我有点焦虑，想要建议\"}" "${AUTHZ_MP[@]}")"
assert_api_ok "$MSG"

set +e
SSE="$(curl -sS -N --max-time 60 "${BASE_URL}/api/v1/miniprogram/ai/chat/sessions/${SESSION_ID}/stream" "${AUTHZ_MP[@]}" 2>/dev/null)"
CURL_SSE_STATUS="$?"
set -e
if [ "$CURL_SSE_STATUS" != "0" ] && [ "$CURL_SSE_STATUS" != "18" ] && [ "$CURL_SSE_STATUS" != "23" ]; then
  echo "curl SSE failed with status=$CURL_SSE_STATUS" >&2
  exit 1
fi
echo "$SSE" | rg -q "event:delta" || {
  echo "SSE missing 'event:delta' (curl status=$CURL_SSE_STATUS)" >&2
  echo "$SSE" >&2
  exit 1
}
echo "$SSE" | rg -q "event:done" || {
  echo "SSE missing 'event:done' (curl status=$CURL_SSE_STATUS)" >&2
  echo "$SSE" >&2
  exit 1
}

echo "== Admin query endpoints =="
assert_api_ok "$(curl_json GET "${BASE_URL}/api/v1/admin/users?page=1&pageSize=20" "${AUTHZ_ADMIN[@]}")"
assert_api_ok "$(curl_json GET "${BASE_URL}/api/v1/admin/orders?page=1&pageSize=20" "${AUTHZ_ADMIN[@]}")"
assert_api_ok "$(curl_json GET "${BASE_URL}/api/v1/admin/assessments?page=1&pageSize=20" "${AUTHZ_ADMIN[@]}")"

echo "== Cleanup (best-effort) =="
curl -sS -X DELETE "${BASE_URL}/api/v1/miniprogram/children/${CHILD_ID}" "${AUTHZ_MP[@]}" >/dev/null || true
curl -sS -X DELETE "${BASE_URL}/api/v1/admin/questions/${IMPORTED_Q_ID}" "${AUTHZ_ADMIN[@]}" >/dev/null || true
for qid in "${QUESTION_IDS[@]}"; do
  curl -sS -X DELETE "${BASE_URL}/api/v1/admin/questions/${qid}" "${AUTHZ_ADMIN[@]}" >/dev/null || true
done
curl -sS -X DELETE "${BASE_URL}/api/v1/admin/quotes/${QUOTE_ID}" "${AUTHZ_ADMIN[@]}" >/dev/null || true
curl -sS -X DELETE "${BASE_URL}/api/v1/admin/plans/${PLAN_ID}" "${AUTHZ_ADMIN[@]}" >/dev/null || true

echo "ALL SELFTEST PASSED"
