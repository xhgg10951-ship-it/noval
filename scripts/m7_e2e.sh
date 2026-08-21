#!/usr/bin/env bash
# M7 Five-Chapter End-to-End Acceptance (PASS-01..12) against live stack.
set -u
B=http://localhost:8080
LOG=/tmp/m7_e2e2.log
: > "$LOG"
jqv() { python3 -c "import sys,json
try:
    d=json.load(sys.stdin)
    print(d.get('$1',''))
except Exception:
    print('')" 2>/dev/null; }
post() { curl -s -m 20 -X POST "$@"; }

echo "===== M7 E2E START $(date) =====" | tee -a "$LOG"

CORE="一名修仙世界的天帝意外穿越到西幻魔法世界。他保留部分修仙能力，但不了解这个世界的魔法体系和社会规则。"
DIR="主角暂时隐藏真实身份，先了解这个陌生世界，并尝试融入当地社会。"
CREATE=$(cat <<JSON
{
  "name": "M7验收故事-天帝穿西幻",
  "coreIdea": "$CORE",
  "initialStageDirection": "$DIR",
  "constraints": [
    {"type":"NarrativePerspective","content":"全文使用第三人称。","sortOrder":1},
    {"type":"WritingStyle","content":"整体风格以轻松、爽文式网文为主。","sortOrder":2},
    {"type":"MagicKnowledge","content":"主角一开始不了解西幻世界的魔法体系。","sortOrder":3},
    {"type":"Identity","content":"当地人不知道主角原本是修仙世界天帝。","sortOrder":4},
    {"type":"WorldRule","content":"当地普通人普遍认为力量体系建立在魔力基础之上。","sortOrder":5}
  ]
}
JSON
)
SID=$(post "$B/api/stories" -H "Content-Type: application/json" -d "$CREATE" | jqv id)
echo "[AT-A01] created story id=$SID" | tee -a "$LOG"

# ---- Stage 1: CONTINUOUS, target 5 chapters ----
PLANREQ=$(cat <<JSON
{"direction":"$DIR","targetChapterCount":5}
JSON
)
STAGE1=$(post "$B/api/stories/$SID/stages" -H "Content-Type: application/json" -d "$PLANREQ")
STAGE1_ID=$(echo "$STAGE1" | jqv id)
echo "[AT-B01/PASS-03] stage1 id=$STAGE1_ID" | tee -a "$LOG"
echo "$STAGE1" | head -c 300 >> "$LOG"; echo >> "$LOG"
[ -z "$STAGE1_ID" ] && { echo "STAGE1 CREATE FAILED"; cat "$LOG"; exit 1; }

CSTAT=$(curl -s -m 10 -o /dev/null -w "%{http_code}" -X POST "$B/api/stages/$STAGE1_ID/confirm")
echo "[confirm] status=$CSTAT" | tee -a "$LOG"

GEN=$(post "$B/api/stages/$STAGE1_ID/generate?mode=CONTINUOUS" -H "Content-Type: application/json" -d '{}')
JOB1=$(echo "$GEN" | jqv id)
echo "[PASS-12 CONTINUOUS] job1=$JOB1" | tee -a "$LOG"
echo "$GEN" | head -c 200 >> "$LOG"; echo >> "$LOG"

ST=""
for i in $(seq 1 40); do
  J=$(curl -s -m 10 "$B/api/generation-jobs/$JOB1")
  ST=$(echo "$J" | jqv status)
  CPI=$(echo "$J" | jqv currentPlanIndex)
  TOT=$(echo "$J" | jqv total)
  echo "  job1 poll $i -> $ST ($CPI/$TOT)" >> "$LOG"
  if [ "$ST" = "COMPLETED" ] || [ "$ST" = "FAILED" ]; then break; fi
  sleep 2
done
echo "[PASS-01] job1 final status=$ST" | tee -a "$LOG"

CH1=$(curl -s -m 10 "$B/api/stages/$STAGE1_ID/chapters" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))" 2>/dev/null)
echo "[PASS-01] stage1 chapter count=$CH1" | tee -a "$LOG"

# ---- Memory state ----
MEM=$(curl -s -m 10 "$B/api/stories/$SID/memory")
echo "$MEM" | head -c 700 >> "$LOG"; echo >> "$LOG"
AUTO_LOC=$(echo "$MEM" | python3 -c "import sys,json;d=json.load(sys.stdin);cs=d.get('currentState',[]);print([c['value'] for c in cs if c.get('field')=='location'])" 2>/dev/null)
AUTO_INV=$(echo "$MEM" | python3 -c "import sys,json;d=json.load(sys.stdin);cs=d.get('currentState',[]);print([c['value'] for c in cs if c.get('field')=='inventory'])" 2>/dev/null)
echo "[PASS-04/06] current_state location=$AUTO_LOC inventory=$AUTO_INV" | tee -a "$LOG"

REL_ID=$(echo "$MEM" | python3 -c "import sys,json;d=json.load(sys.stdin);cands=d.get('candidates',[]);rid=[c['id'] for c in cands if c.get('type')=='RELATIONSHIP' and c.get('processingStatus')=='REVIEW'];print(rid[0] if rid else '')" 2>/dev/null)
if [ -n "$REL_ID" ]; then
  curl -s -m 10 -X POST "$B/api/memory/candidates/$REL_ID/apply" -o /dev/null -w "[PASS-07] applied REVIEW relationship candidate id=$REL_ID status=%{http_code}\n" | tee -a "$LOG"
else
  echo "[PASS-07] no REVIEW relationship candidate (note)" | tee -a "$LOG"
fi

FS_ID=$(echo "$MEM" | python3 -c "import sys,json;d=json.load(sys.stdin);cands=d.get('candidates',[]);fid=[c['id'] for c in cands if c.get('type')=='STORY_MEMORY'];print(fid[0] if fid else '')" 2>/dev/null)
if [ -n "$FS_ID" ]; then
  curl -s -m 10 -X POST "$B/api/memory/candidates/$FS_ID/ignore" -o /dev/null -w "[PASS-08] author override IGNORE on candidate id=$FS_ID status=%{http_code}\n" | tee -a "$LOG"
else
  echo "[PASS-08] no STORY_MEMORY candidate to override (note)" | tee -a "$LOG"
fi

QLOC=$(curl -s -m 10 -X POST "$B/api/stories/$SID/story-query" -H "Content-Type: application/json" -d '{"question":"主角现在在哪里"}' | jqv answer)
QINV=$(curl -s -m 10 -X POST "$B/api/stories/$SID/story-query" -H "Content-Type: application/json" -d '{"question":"主角持有什么物品"}' | jqv answer)
echo "[PASS-10] query location='$QLOC' inventory='$QINV'" | tee -a "$LOG"

# ---- Stage 2: STEP-BY-STEP ----
STAGE2=$(post "$B/api/stories/$SID/stages" -H "Content-Type: application/json" -d '{"direction":"围绕玉佩线索展开新的小冲突，测试逐章暂停。","targetChapterCount":2}')
STAGE2_ID=$(echo "$STAGE2" | jqv id)
CSTAT2=$(curl -s -m 10 -o /dev/null -w "%{http_code}" -X POST "$B/api/stages/$STAGE2_ID/confirm")
echo "[PASS-12 STEP] stage2=$STAGE2_ID confirm=$CSTAT2" | tee -a "$LOG"
JOB2=$(post "$B/api/stages/$STAGE2_ID/generate?mode=STEP" -H "Content-Type: application/json" -d '{}' | jqv id)
echo "[PASS-12 STEP] job2=$JOB2" | tee -a "$LOG"
for i in $(seq 1 10); do
  J=$(curl -s -m 10 "$B/api/generation-jobs/$JOB2")
  ST2=$(echo "$J" | jqv status)
  echo "  job2 step $i -> $ST2" >> "$LOG"
  if [ "$ST2" = "PAUSED" ]; then
    curl -s -m 15 -X POST "$B/api/generation-jobs/$JOB2/continue" -o /dev/null -w "  continue=%{http_code}\n" >> "$LOG"
  elif [ "$ST2" = "COMPLETED" ] || [ "$ST2" = "FAILED" ]; then
    break
  fi
  sleep 2
done
echo "[PASS-12 STEP] job2 final=$(curl -s -m 10 "$B/api/generation-jobs/$JOB2" | jqv status)" | tee -a "$LOG"

# ---- Planner Suggestions ----
SUG=$(curl -s -m 10 -X POST "$B/api/stories/$SID/suggest-directions" -H "Content-Type: application/json" -d '{}')
NDIR=$(echo "$SUG" | python3 -c "import sys,json;print(len(json.load(sys.stdin).get('directions',[])))" 2>/dev/null)
echo "[PASS-11] suggest-directions returned $NDIR directions" | tee -a "$LOG"
echo "$SUG" | head -c 250 >> "$LOG"; echo >> "$LOG"

echo "===== M7 E2E END $(date) =====" | tee -a "$LOG"
echo "STORY_ID=$SID" > /tmp/m7_story.txt
