# -*- coding: utf-8 -*-
"""TASK-171 / AC-109 — Real-LLM polish acceptance.

Fact-preservation check on the polished output:
plot / location / inventory / ending intent must all survive.
"""
import json
import sys
import urllib.request

BASE = "http://127.0.0.1:8000"

request = {
    "content": (
        "林夜和艾琳走进了幽影森林。森林里很黑。林夜拔出铁剑。艾琳说：小心点，这里不对劲。"
        "他们走了很久。后来他们发现了一个山洞。山洞里有一些痕迹。总之这一天发生了很多事情，"
        "总的来说是一次难忘的经历。最后他们决定先回去。"
    ),
    "chapterGoal": "调查幽影森林失踪案，发现失踪者留下的线索",
    "endingIntent": "以发现关键线索收尾，为下一章深入调查留接口",
    "constraints": [{"type": "风格", "content": "冷峻克制"}],
    "currentState": [
        {"category": "INVENTORY", "subject": "林夜", "field": "item:铁剑", "value": "铁剑"},
        {"category": "LOCATION", "subject": "林夜", "field": "location", "value": "幽影森林"},
    ],
    "writingStyle": "冷峻克制，多用具体感官细节，少用形容词堆砌",
    "userInstruction": "把机械总结式的结尾改掉，让对话更自然。",
}

req = urllib.request.Request(
    BASE + "/ai/polish-chapter",
    data=json.dumps(request).encode("utf-8"),
    headers={"Content-Type": "application/json"},
    method="POST",
)
with urllib.request.urlopen(req, timeout=300) as resp:
    out = json.loads(resp.read().decode("utf-8"))

with open(".agent/evidence/ac109_polish.json", "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=2)

polished = out.get("polishedContent") or ""
print("--- polished ---")
print(polished[:600])

# ---- structured fact-preservation checks ----
checks = {
    "location_preserved": "幽影森林" in polished,
    "inventory_preserved": "铁剑" in polished,
    "character_preserved": ("林夜" in polished and "艾琳" in polished),
    "clue_plot_preserved": ("山洞" in polished or "痕迹" in polished),
    "mechanical_summary_removed": "总的来说" not in polished and "难忘的经历" not in polished,
    "no_new_destination": ("返回公会" not in polished and "回到城镇" not in polished
                           and "回到了城里" not in polished),
}
print("--- checks ---")
ok = True
for name, passed in checks.items():
    print(f"{name}: {'PASS' if passed else 'FAIL'}")
    if not passed:
        ok = False

# ending-intent preserved = chapter still ENDS with discovery/clue, not a wrap-up.
tail = polished[-120:]
ends_with_discovery = ("线索" in tail) or ("痕迹" in tail) or ("山洞" in tail) or ("微光" in tail)
print(f"ending_intent_preserved: {'PASS' if ends_with_discovery else 'FAIL'}")
if not ends_with_discovery:
    ok = False

print("VERDICT:", "PASS" if ok else "FAIL")
sys.exit(0 if ok else 1)
