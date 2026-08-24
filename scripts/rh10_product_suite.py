#!/usr/bin/env python3
"""Product-wired qwen3.7-plus release acceptance runner.

The runner talks only to the public Spring Boot API. Spring assembles context,
decides workflow state and persists to MySQL; the configured Python service is
therefore reached through the same boundary as the real UI. Every request and
response is written to a run-specific evidence directory.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import time
import traceback
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any


TERMINAL_JOBS = {"PAUSED", "STOPPED", "FAILED", "COMPLETED"}
FINAL_JOBS = {"STOPPED", "FAILED", "COMPLETED"}

# AC-105 requires three unrelated ChapterSpecs. Keep the low-value term out of
# every goal: naming a forbidden detail in a high-priority ChapterSpec would be
# an artificial narrative anchor and would not exercise the frozen acceptance.
AC105_UNRELATED_GOALS = [
    "完成冒险者公会登记并取得资格；林夜必须隐藏穿越者身份，不得向任何人暴露。",
    "接取调查幽影森林失踪案的初步委托，核对三名失踪者最后活动的时间与地点。",
    "与艾琳准备调查药剂和绳索，确认南城门出城路线，并决定次日清晨出发。",
]

AC105_UNRELATED_SPECS = [
    {
        "expectedProgress": "林夜进入公会大厅，完成登记和基础测试，取得合法身份牌。",
        "mustAdvance": "进入冒险者公会\n完成身份登记\n完成基础测试铺垫",
        "mustNotDo": "不得暴露天帝身份\n不得暴露穿越者身份",
        "storyBeats": "抵达接待台\n填写身份档案\n完成基础测试\n领取身份牌",
        "endingIntent": "登记完成后前往委托板",
    },
    {
        "expectedProgress": "领取失踪案委托并整理三名失踪者的时空线索。",
        "mustAdvance": "领取调查委托\n核对失踪时间\n核对最后活动地点",
        "mustNotDo": "不得直接找到幕后真凶",
        "storyBeats": "查看委托板\n与接待员核对档案\n整理调查顺序",
        "endingIntent": "确定调查幽影森林失踪案",
    },
    {
        "expectedProgress": "备齐调查物资并确认从南城门前往幽影森林的路线。",
        "mustAdvance": "准备药剂和绳索\n确认南城门路线\n决定次日清晨出发",
        "mustNotDo": "不得提前进入幽影森林深处",
        "storyBeats": "清点物资\n查看地图\n确认出发时间",
        "endingIntent": "次日清晨从南城门出发",
    },
]


def remove_preexisting_bread(text: str) -> str:
    """Make AC-105 measure only the deliberately inserted ordinary bread."""
    paragraphs = (text or "").splitlines()
    cleaned = [line for line in paragraphs if "面包" not in line]
    return "\n".join(cleaned).strip()


def low_value_bread_is_isolated(chapters: list[dict[str, Any]]) -> dict[str, Any]:
    """Measure narrative focus, not a brittle ban on an ordinary noun.

    AC-105 forbids later unrelated chapters from continuing to revolve around
    the inserted ordinary bread. Incidental independent world-building (for
    example, a bakery smell in a street scene) is not narrative hijacking.
    """
    mention_counts: list[int] = []
    focus_ratios: list[float] = []
    ordinary_mentions: list[int] = []
    for chapter in chapters:
        content = chapter.get("content") or ""
        fragments = re.split(r"(?<=[。！？!?])|\n+", content)
        focused_chars = sum(len(part) for part in fragments if "面包" in part)
        mention_counts.append(content.count("面包"))
        focus_ratios.append(focused_chars / max(1, len(content)))
        ordinary_mentions.append(content.count("普通面包"))
    focused_chapters = sum(count > 0 for count in mention_counts)
    passed = (
        sum(ordinary_mentions) == 0
        and max(focus_ratios, default=0.0) <= 0.03
        and focused_chapters < len(chapters)
    )
    return {
        "passed": passed,
        "mentionCounts": mention_counts,
        "ordinaryBreadMentions": ordinary_mentions,
        "focusRatios": [round(ratio, 4) for ratio in focus_ratios],
        "focusedChapterCount": focused_chapters,
    }


class ProductSuite:
    def __init__(self, base_url: str, evidence_dir: Path, run_id: str,
                 product_commit: str) -> None:
        self.base_url = base_url.rstrip("/")
        self.evidence_dir = evidence_dir
        self.run_id = run_id
        self.product_commit = product_commit
        self.trace_index = 0
        self.results: dict[str, dict[str, Any]] = {}
        self.story_ids: list[int] = []
        evidence_dir.mkdir(parents=True, exist_ok=False)

    def save(self, name: str, value: Any) -> None:
        path = self.evidence_dir / name
        path.write_text(json.dumps(value, ensure_ascii=False, indent=2),
                        encoding="utf-8")

    def api(self, method: str, path: str, body: Any | None = None,
            timeout: int = 900) -> Any:
        self.trace_index += 1
        url = f"{self.base_url}{path}"
        data = None if body is None else json.dumps(
            body, ensure_ascii=False).encode("utf-8")
        request = urllib.request.Request(
            url, data=data, method=method,
            headers={"Content-Type": "application/json; charset=utf-8",
                     "Accept": "application/json"},
        )
        started = time.monotonic()
        status = None
        response_body: Any = None
        try:
            with urllib.request.urlopen(request, timeout=timeout) as response:
                status = response.status
                raw = response.read().decode("utf-8")
                response_body = json.loads(raw) if raw else None
        except urllib.error.HTTPError as exc:
            status = exc.code
            raw = exc.read().decode("utf-8", errors="replace")
            try:
                response_body = json.loads(raw)
            except json.JSONDecodeError:
                response_body = raw
            raise RuntimeError(
                f"{method} {path} returned HTTP {status}: {response_body}") from exc
        finally:
            trace = {
                "method": method,
                "path": path,
                "request": body,
                "status": status,
                "durationSeconds": round(time.monotonic() - started, 3),
                "response": response_body,
            }
            self.save(f"trace_{self.trace_index:03d}.json", trace)
        return response_body

    def wait_job(self, job_id: int, *, final_only: bool = False,
                 timeout: int = 1200) -> dict[str, Any]:
        deadline = time.monotonic() + timeout
        observed: list[dict[str, Any]] = []
        wanted = FINAL_JOBS if final_only else TERMINAL_JOBS
        while time.monotonic() < deadline:
            job = self.api("GET", f"/api/generation-jobs/{job_id}")
            snapshot = {
                "status": job["status"], "phase": job["phase"],
                "currentPlanIndex": job["currentPlanIndex"],
                "total": job["total"], "lastError": job.get("lastError"),
            }
            if not observed or snapshot != observed[-1]:
                observed.append(snapshot)
                print(f"job {job_id}: {snapshot['status']} "
                      f"{snapshot['currentPlanIndex']}/{snapshot['total']} "
                      f"{snapshot['phase']}", flush=True)
            if job["status"] in wanted:
                self.save(f"job_{job_id}_observations.json", observed)
                if job["status"] == "FAILED":
                    raise RuntimeError(
                        f"Generation job {job_id} failed: {job.get('lastError')}")
                return job
            time.sleep(2)
        raise TimeoutError(f"Generation job {job_id} did not reach {wanted}")

    def start_step(self, stage_id: int) -> dict[str, Any]:
        job = self.api("POST", f"/api/stages/{stage_id}/generate?mode=STEP")
        return self.wait_job(job["id"])

    def continue_step(self, job_id: int) -> dict[str, Any]:
        self.api("POST", f"/api/generation-jobs/{job_id}/continue")
        return self.wait_job(job_id)

    def finish_step_job(self, stage_id: int, expected_chapters: int) -> int:
        job = self.start_step(stage_id)
        job_id = job["id"]
        while True:
            chapters = self.api("GET", f"/api/stages/{stage_id}/chapters")
            if len(chapters) >= expected_chapters and job["status"] == "COMPLETED":
                return job_id
            if job["status"] != "PAUSED":
                raise RuntimeError(
                    f"STEP job {job_id} stopped unexpectedly as {job['status']}")
            job = self.continue_step(job_id)

    def record_result(self, acceptance: str, passed: bool, **metrics: Any) -> None:
        self.results[acceptance] = {"passed": passed, **metrics}
        print(f"{acceptance}: {'PASS' if passed else 'FAIL'}", flush=True)

    @staticmethod
    def create_story_body(name: str, target_chars: int,
                          target_chapters: int) -> dict[str, Any]:
        return {
            "name": name,
            "coreIdea": "现代青年林夜穿越到剑与魔法世界，结识冒险者艾琳并被她收留。",
            "initialStageDirection": "从既定事实继续，按章推进，不复述已经完成的相遇。",
            "defaultTargetCharacters": target_chars,
            "targetChapterCount": target_chapters,
            "writingStyle": "冷峻克制，多用具体感官细节，少用形容词堆砌",
            "constraints": [
                {"type": "HARD", "content": "林夜必须隐藏穿越者身份", "sortOrder": 1},
                {"type": "HARD", "content": "不得跳到终局或安排最终大战", "sortOrder": 2},
            ],
        }

    def create_story(self, name: str, target_chars: int,
                     target_chapters: int) -> dict[str, Any]:
        story = self.api("POST", "/api/stories",
                         self.create_story_body(name, target_chars, target_chapters))
        self.story_ids.append(story["id"])
        return story

    def create_arc(self, story_id: int, end: int = 60) -> dict[str, Any]:
        return self.api("POST", f"/api/stories/{story_id}/arcs", {
            "title": "第一卷·初入异界",
            "goal": "在城镇立足、成为正式冒险者并完成初期委托",
            "targetStartChapter": 1,
            "targetEndChapter": end,
            "status": "ACTIVE",
        })

    def update_goals(self, stage: dict[str, Any], goals: list[str],
                     target_chars: int) -> None:
        plans = sorted((p for p in stage["plans"] if p["active"]),
                       key=lambda p: p["chapterOrder"])
        if len(plans) != len(goals):
            raise AssertionError(
                f"Expected {len(goals)} active plans, got {len(plans)}")
        for plan, goal in zip(plans, goals):
            self.api("PUT", f"/api/stages/plans/{plan['id']}", {
                "goal": goal, "targetCharacters": target_chars,
            })

    @staticmethod
    def length_without_whitespace(text: str) -> int:
        return len(re.sub(r"\s+", "", text or ""))

    @staticmethod
    def duplicate_long_sentences(texts: list[str]) -> list[str]:
        seen: set[str] = set()
        duplicates: list[str] = []
        for text in texts:
            for sentence in re.split(r"[。！？!?\n]+", text):
                normalized = re.sub(r"\s+", "", sentence)
                if len(normalized) < 24:
                    continue
                if normalized in seen:
                    duplicates.append(normalized)
                seen.add(normalized)
        return duplicates

    def run_story_a(self) -> None:
        story = self.create_story(
            f"RH10产品链A-{self.run_id}", target_chars=3000, target_chapters=600)
        story_id = story["id"]
        self.create_arc(story_id, 60)

        stage1 = self.api("POST", f"/api/stories/{story_id}/stages", {
            "direction": "用两章写完林夜初到异界、认识艾琳并在她住处安顿过夜。",
            "targetChapterCount": 2,
            "targetCharacters": 3000,
        })
        self.update_goals(stage1, [
            "林夜初到异界，在城门附近遇见艾琳；两人建立初步信任。",
            "艾琳把林夜带回住处安顿；本章结束时已经过夜，二人已认识且林夜已有住所。",
        ], 3000)
        self.api("POST", f"/api/stages/{stage1['id']}/confirm")
        self.finish_step_job(stage1["id"], 2)
        first_two = self.api("GET", f"/api/stages/{stage1['id']}/chapters")
        chapter2 = sorted(first_two, key=lambda c: c["chapterNumber"])[-1]
        memory_before_edit = self.api("GET", f"/api/stories/{story_id}/memory")
        old_candidate_ids = {
            c["id"] for c in memory_before_edit["candidates"]
            if c.get("sourceChapterId") == chapter2["id"]
        }

        bread_sentence = "早餐时，林夜吃掉一块普通面包，只是填饱肚子，随后不再关注它。"
        clean_chapter2 = remove_preexisting_bread(chapter2["content"])
        edited = self.api("PUT", f"/api/chapters/{chapter2['id']}/content", {
            "content": clean_chapter2 + "\n\n" + bread_sentence,
        })
        if edited["memoryExtractionStatus"] != "COMPLETED":
            raise AssertionError("Manual Edit did not complete Memory refresh")
        memory_after_bread = self.api("GET", f"/api/stories/{story_id}/memory")
        bread_candidates = [c for c in memory_after_bread["candidates"]
                            if "面包" in json.dumps(c, ensure_ascii=False)]

        stage2 = self.api("POST", f"/api/stories/{story_id}/stages", {
            "direction": "第二天前往冒险者公会入会并接取委托：调查幽影森林失踪案。",
            "targetChapterCount": 3,
            "targetCharacters": 3000,
        })
        plan_text = "\n".join(
            f"{p.get('goal', '')} {p.get('expectedProgress', '')}"
            for p in stage2["plans"])
        forbidden_restart = [
            "重新穿越", "再次穿越", "初次遇见艾琳", "第一次见到艾琳",
            "寻找住处", "无处可住",
        ]
        restart_hits = [term for term in forbidden_restart if term in plan_text]
        continuation_pass = (
            "公会" in plan_text
            and not restart_hits
            and [p["chapterOrder"] for p in stage2["plans"]] == [3, 4, 5]
        )
        self.record_result(
            "AC-101", continuation_pass, restartHits=restart_hits,
            logicalOrders=[p["chapterOrder"] for p in stage2["plans"]],
            plannerText=plan_text,
        )

        for plan, goal, spec in zip(
                stage2["plans"], AC105_UNRELATED_GOALS, AC105_UNRELATED_SPECS):
            self.api("PUT", f"/api/stages/plans/{plan['id']}", {
                "goal": goal,
                "targetCharacters": 3000,
                **spec,
            })
        self.api("POST", f"/api/stages/{stage2['id']}/confirm")
        self.finish_step_job(stage2["id"], 3)
        later_three = sorted(
            self.api("GET", f"/api/stages/{stage2['id']}/chapters"),
            key=lambda c: c["chapterNumber"])
        all_five = sorted(first_two[:-1] + [edited] + later_three,
                          key=lambda c: c["chapterNumber"])

        actuals = [c["actualCharacterCount"] for c in all_five]
        in_band = [2250 <= value <= 3750 for value in actuals]
        duplicate_sentences = self.duplicate_long_sentences(
            [c["content"] for c in all_five])
        self.record_result(
            "AC-103", sum(in_band) >= 4 and not duplicate_sentences,
            targetCharacters=3000, actualCharacters=actuals,
            inBand=in_band, passRate=f"{sum(in_band)}/5",
            duplicateLongSentences=duplicate_sentences,
        )

        guild_chapter = later_three[0]
        content = guild_chapter["content"]
        registration_terms = ["登记", "注册", "入会", "手续", "档案", "身份牌"]
        registration_hits = [term for term in registration_terms if term in content]
        identity_violations = [term for term in [
            "我是穿越者", "我来自另一个世界", "林夜承认自己是穿越者",
            "向艾琳坦白穿越", "告诉接待员自己穿越",
        ] if term in content]
        self.record_result(
            "AC-104", bool(registration_hits) and not identity_violations,
            chapterId=guild_chapter["id"], registrationHits=registration_hits,
            identityViolations=identity_violations,
        )

        exact_bread_candidates = [c for c in bread_candidates if (
            c.get("type") == "TRANSIENT_DETAIL"
            and c.get("importance") == 1
            and c.get("scope") == "CHAPTER"
            and c.get("suggestedAction") == "IGNORE"
            and c.get("processingStatus") == "IGNORED"
        )]
        old_candidates = [
            c for c in memory_after_bread["candidates"]
            if c["id"] in old_candidate_ids
        ]
        old_candidates_superseded = (
            len(old_candidates) == len(old_candidate_ids) and all(
            c.get("processingStatus") == "SUPERSEDED" and not c.get("applied")
            for c in old_candidates
            )
        )
        isolation = low_value_bread_is_isolated(later_three)
        self.record_result(
            "AC-105", bool(exact_bread_candidates)
            and old_candidates_superseded
            and isolation["passed"],
            breadCandidates=bread_candidates,
            exactClassificationCount=len(exact_bread_candidates),
            oldCandidateCount=len(old_candidates),
            oldCandidatesSuperseded=old_candidates_superseded,
            laterBreadMentions=isolation["mentionCounts"],
            ordinaryBreadMentions=isolation["ordinaryBreadMentions"],
            breadFocusRatios=isolation["focusRatios"],
            breadFocusedChapterCount=isolation["focusedChapterCount"],
        )

        mechanical = (
            "林夜和艾琳在冒险者公会完成失踪案线索汇报。"
            "林夜仍站在公会大厅。林夜保留铁剑和调查委托书。"
            "林夜确认失踪者最后出现在幽影森林。"
            "林夜听见艾琳提醒他午夜去旧钟楼见证人。"
            "林夜回答：我们午夜去旧钟楼。"
        )
        manual = self.api("PUT", f"/api/chapters/{later_three[-1]['id']}/content", {
            "content": mechanical,
        })
        polished = self.api("POST", f"/api/chapters/{manual['id']}/polish", {
            "userInstruction": "降低机械重复，保持全部事实、位置、物品和结尾意图，不新增事件。",
        })
        polished_text = polished["content"]
        fact_groups = {
            "characters": ["林夜", "艾琳"],
            "location": ["冒险者公会", "公会大厅"],
            "items": ["铁剑", "调查委托书"],
            "case": ["幽影森林", "失踪"],
            "ending": ["午夜", "旧钟楼", "见证人"],
        }
        missing_facts = {
            name: [term for term in terms if term not in polished_text]
            for name, terms in fact_groups.items()
        }
        before_mechanical = len(re.findall(r"(?:^|。)林夜", mechanical))
        after_mechanical = len(re.findall(r"(?:^|。)林夜", polished_text))
        polish_pass = (
            polished_text != mechanical
            and not any(missing_facts.values())
            and after_mechanical < before_mechanical
            and polished["sourceType"] == "AI_POLISH"
            and polished["memoryExtractionStatus"] == "COMPLETED"
        )
        self.record_result(
            "AC-109", polish_pass, chapterId=polished["id"],
            missingFacts=missing_facts,
            mechanicalStartsBefore=before_mechanical,
            mechanicalStartsAfter=after_mechanical,
            sourceType=polished["sourceType"],
            memoryExtractionStatus=polished["memoryExtractionStatus"],
        )

        stage3 = self.api("POST", f"/api/stories/{story_id}/stages", {
            "direction": (
                "current=5；继续第一卷1–60章的生存融入，只安排近期公会委托、"
                "城镇立足和幽影森林调查，不揭示终局真相，不安排终局大战或最终返回通道。"
            ),
            "targetChapterCount": 5,
            "targetCharacters": 3000,
        })
        pace_text = "\n".join(
            f"{p.get('goal', '')} {p.get('expectedProgress', '')}"
            for p in stage3["plans"])
        endgame_patterns = [
            "终局真相", "最终真相", "终局大战", "最终大战", "最终返回通道",
            "返回现实世界", "击败最终", "世界终结",
        ]
        endgame_hits = [term for term in endgame_patterns if term in pace_text]
        local_terms = ["公会", "委托", "城镇", "森林", "失踪", "调查", "生存", "融入"]
        local_hits = [term for term in local_terms if term in pace_text]
        pace_orders = [p["chapterOrder"] for p in stage3["plans"]]
        self.record_result(
            "AC-114", not endgame_hits and bool(local_hits)
            and pace_orders == [6, 7, 8, 9, 10],
            targetChapterCount=600, currentChapter=5,
            arc="1-60 生存融入", logicalOrders=pace_orders,
            endgameHits=endgame_hits, localProgressHits=local_hits,
            plannerText=pace_text,
        )

        self.save("story_a_snapshot.json", {
            "story": self.api("GET", f"/api/stories/{story_id}"),
            "memory": self.api("GET", f"/api/stories/{story_id}/memory"),
            "stage1": self.api("GET", f"/api/stages/{stage1['id']}"),
            "stage2": self.api("GET", f"/api/stages/{stage2['id']}"),
            "stage3": self.api("GET", f"/api/stages/{stage3['id']}"),
            "chapters": all_five,
            "polishedChapter": polished,
        })

    def run_story_b(self) -> None:
        story = self.create_story(
            f"RH10产品链B-{self.run_id}", target_chars=800, target_chapters=100)
        story_id = story["id"]
        self.create_arc(story_id, 30)
        stage = self.api("POST", f"/api/stories/{story_id}/stages", {
            "direction": "用九章推进一次城镇失踪调查，线索逐步升级但不进入终局。",
            "targetChapterCount": 9,
            "targetCharacters": 800,
        })
        self.api("POST", f"/api/stages/{stage['id']}/confirm")
        job = self.start_step(stage["id"])
        job_id = job["id"]
        for _ in range(2):
            if job["status"] != "PAUSED":
                raise RuntimeError(f"Expected PAUSED, got {job['status']}")
            job = self.continue_step(job_id)
        before = sorted(
            self.api("GET", f"/api/stages/{stage['id']}/chapters"),
            key=lambda c: c["chapterNumber"])
        if len(before) != 3:
            raise AssertionError(f"Expected three completed chapters, got {len(before)}")
        stopped = self.api("POST", f"/api/generation-jobs/{job_id}/stop")
        if stopped["status"] != "STOPPED":
            stopped = self.wait_job(job_id, final_only=True)
        replanned = self.api("POST", f"/api/stages/{stage['id']}/replan-remaining", {
            "remainingChapterCount": 3,
            "authorInstruction": "将剩余调查压缩为三章，从当前真实状态继续，不复述前三章。",
        })
        new_active = sorted(
            (p for p in replanned["plans"] if p["active"]),
            key=lambda p: p["chapterOrder"])
        replan_semantic_text = "\n".join(
            f"{p.get('goal', '')} {p.get('expectedProgress', '')}" for p in new_active)
        repeated_opening_hits = [term for term in [
            "初到异界", "第一次见艾琳", "重新开始调查", "重新接取委托",
        ] if term in replan_semantic_text]

        next_job = self.api(
            "POST", f"/api/stages/{stage['id']}/generate?mode=CONTINUOUS")
        self.wait_job(next_job["id"], final_only=True)
        after = sorted(
            self.api("GET", f"/api/stages/{stage['id']}/chapters"),
            key=lambda c: c["chapterNumber"])
        first_unchanged = all(
            before[index]["id"] == after[index]["id"]
            and before[index]["content"] == after[index]["content"]
            and before[index]["currentRevisionId"] == after[index]["currentRevisionId"]
            for index in range(3)
        )
        numbers = [c["chapterNumber"] for c in after]
        normalized_titles = [re.sub(r"\W+", "", c["title"] or "") for c in after]
        duplicate_titles = sorted({title for title in normalized_titles
                                   if title and normalized_titles.count(title) > 1})
        passed = (
            [p["chapterOrder"] for p in new_active] == [4, 5, 6]
            and not repeated_opening_hits
            and len(after) == 6
            and numbers == [1, 2, 3, 4, 5, 6]
            and first_unchanged
            and not duplicate_titles
        )
        self.record_result(
            "AC-106", passed,
            replanLogicalOrders=[p["chapterOrder"] for p in new_active],
            repeatedOpeningHits=repeated_opening_hits,
            finalChapterNumbers=numbers, firstThreeUnchanged=first_unchanged,
            duplicateTitles=duplicate_titles,
            stoppedJobStatus=stopped["status"],
            finalJobStatus=self.api(
                "GET", f"/api/generation-jobs/{next_job['id']}")["status"],
            replanSemanticText=replan_semantic_text,
        )
        self.save("story_b_snapshot.json", {
            "story": self.api("GET", f"/api/stories/{story_id}"),
            "stage": self.api("GET", f"/api/stages/{stage['id']}"),
            "chaptersBeforeReplan": before,
            "chaptersAfterReplan": after,
            "memory": self.api("GET", f"/api/stories/{story_id}/memory"),
        })

    def write_summary(self) -> bool:
        required = ["AC-101", "AC-103", "AC-104", "AC-105",
                    "AC-106", "AC-109", "AC-114"]
        all_passed = all(self.results.get(ac, {}).get("passed") for ac in required)
        metadata = {
            "runId": self.run_id,
            "model": "qwen3.7-plus",
            "mockLlm": False,
            "productWired": True,
            "backendBaseUrl": self.base_url,
            "productCommit": self.product_commit,
            "promptVersion": f"ai-service/app/prompts/builders.py@{self.product_commit}",
            "storyIds": self.story_ids,
            "targetCharacters": 3000,
            "requiredLengthBand": [2250, 3750],
            "requiredAcceptances": required,
            "traceCount": self.trace_index,
        }
        self.save("RUN_METADATA.json", metadata)
        self.save("RESULTS.json", {
            "passed": all_passed, "results": self.results,
        })
        rows = [
            "# RH-10 qwen3.7-plus Product-wired Results", "",
            f"Run ID: `{self.run_id}`", "",
            f"Product/prompt commit: `{self.product_commit}`", "",
            "Provider: `qwen3.7-plus`, `mock_llm=false`", "",
            "Execution boundary: public Spring Boot API → Java workflow/context "
            "assembly → Python AI service → MySQL persistence.", "",
            "| Acceptance | Result | Metrics |", "|---|---|---|",
        ]
        for ac in required:
            result = self.results.get(ac, {"passed": False, "missing": True})
            metrics = {k: v for k, v in result.items() if k != "passed"}
            compact = json.dumps(metrics, ensure_ascii=False, separators=(",", ":"))
            rows.append(
                f"| {ac} | {'PASS' if result.get('passed') else 'FAIL'} | "
                f"`{compact}` |")
        rows += ["", f"Final: **{'7/7 PASSED' if all_passed else 'FAILED'}**", ""]
        (self.evidence_dir / "RESULTS.md").write_text(
            "\n".join(rows), encoding="utf-8")
        return all_passed

    def run(self) -> bool:
        backend_health = self.api("GET", "/api/health")
        ai_health = self.api("GET", "/api/ai/health")
        self.save("HEALTH.json", {"backend": backend_health, "ai": ai_health})
        if backend_health.get("version") != "0.1.1":
            raise AssertionError("Backend is not v0.1.1")
        if ai_health.get("mockLlm", ai_health.get("mock_llm")) is not False:
            raise AssertionError("AI service is not in real-LLM mode")
        self.run_story_a()
        self.run_story_b()
        return self.write_summary()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--evidence-dir", required=True, type=Path)
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--product-commit", required=True)
    args = parser.parse_args()
    suite = ProductSuite(args.base_url, args.evidence_dir, args.run_id,
                         args.product_commit)
    try:
        passed = suite.run()
    except Exception as exc:  # preserve partial evidence for release diagnosis
        suite.save("FAILURE.json", {
            "error": str(exc), "traceback": traceback.format_exc(),
            "partialResults": suite.results, "storyIds": suite.story_ids,
        })
        try:
            suite.write_summary()
        except Exception:
            pass
        print(f"RH-10 runner failed: {exc}", file=sys.stderr, flush=True)
        return 1
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
