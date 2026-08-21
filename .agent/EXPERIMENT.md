# M7 — Acceptance & Memory Experiment Record

_Updated: 2026-08-21_

## Run metadata

- Model: `MOCK` (no `LLM_API_KEY` configured). Deterministic `MockProvider` for planner/writer/extractor/query.
- Generation Mode: CONTINUOUS (Stage 122, 5 chapters) + STEP (Stage 123, 2 chapters).
- Memory Enabled: YES (structured Current State / Story Memory / Relationship).
- Acceptance Story: fixed from `ACCEPTANCE_TESTS.md` §5/§6 (天帝穿西幻, 5 constraints, seed facts).

## Five-Chapter End-to-End (TASK-048, PASS-01..12)

Live run `scripts/m7_e2e.sh` against Python :8000 + Backend :8080 + Frontend :5173.

| PASS | Criterion | Observed | Result |
|------|-----------|----------|--------|
| PASS-01 | 5 logical chapters | Stage 122 → 5 chapters (CONTINUOUS), Stage 123 → 2 (STEP) | PASS |
| PASS-02 | No re-entry of full background | Context assembled once from Story; chapters generated without re-input | PASS |
| PASS-03 | Direction → multiple chapters | 1 direction → 5-chapter plan | PASS |
| PASS-04 | Current State in generation | Current State assembled into writer request each chapter | PASS (structurally; prose unaffected in MOCK) |
| PASS-05 | Early Story Memory used later/Query | Story Query answers from Current State (memory layer participates) | PASS (Structured Memory; prose reuse needs real LLM) |
| PASS-06 | Memory state change updates Current State | AUTO candidates APPLIED (location/inventory/physical_condition) | PASS |
| PASS-07 | REVIEW candidate confirmed by author | Relationship 艾琳→主角 APPLIED via author `apply` | PASS |
| PASS-08 | IGNORE/AUTO override by author | AUTO location candidate overridden to IGNORE | PASS |
| PASS-09 | Memory traces to Source Chapter + Evidence | candidates carry `sourceChapterId` + `evidence` | PASS |
| PASS-10 | Story Query answers basic state | "当前位置：禁书区最深处。" / "当前持有：生锈的铜钥匙。" | PASS |
| PASS-11 | Planner 3 distinct directions | 冲突型 / 成长型 / 悬疑型 | PASS |
| PASS-12 | Both CONTINUOUS and STEP ran | CONTINUOUS COMPLETED 5/5; STEP PAUSED→continue→COMPLETED | PASS |

## Experiment N1 — Baseline vs Memory (TASK-049..051)

### Version A — Baseline (memory disabled)
- Writer receives: Constraints + Stage Direction + Chapter Goal + recent summary only.
- Expected: no structured Current State / Story Memory rows; Story Query returns Unknown for location/inventory.

### Version B — Memory (memory enabled)
- Writer receives: above + Current State + Story Memory + Relationship.
- Observed (run on Story 200):
  - Current State AUTO-applied: `location=禁书区最深处`, `inventory=生锈的铜钥匙`, `physical_condition=受伤`.
  - Relationship REVIEW: `艾琳→主角 保持警惕/敌意`.
  - Story Query answers location/inventory correctly from structured state.

### Comparison (honest)
- **With MOCK provider**, the deterministic `mock_generate` does NOT inject `currentState`/`storyMemories` into the chapter prose. Therefore the Baseline-vs-Memory difference in MOCK mode is observable ONLY at the **structured memory layer + Story Query**, not in chapter text.
- A real prose-level comparison (does memory reduce state/inventory/relationship errors in generated text?) requires `LLM_API_KEY` (real LangChain provider), which is unavailable in this environment.
- This is recorded as a **known limitation**, not a v0.1 failure. Per `ACCEPTANCE_TESTS.md` §30, "5 章测试不能证明 100 章可靠" and absence of RAG/real-LLM prose comparison do not auto-fail v0.1.

### Recorded observations (per §25)
- Run ID: M7-mock-2026-08-21
- Model: MOCK
- Generation Mode: CONTINUOUS + STEP
- Memory Enabled: YES (Version B verified; Version A behavior inferred from architecture — memory off = no structured rows)
- Number of Chapters: 7 (5 + 2)
- Observed Errors: none at structured layer
- Manual Corrections Required: 0 (author overrides exercised but not required for correctness)
- Notes: structured memory pipeline works end-to-end; prose-level benefit unverified without real LLM.

## Manual Review Answers (§32)
1. Without memory, would 5 chapters show more state errors? — In MOCK mode the writer is self-contained so prose is identical; with a real LLM, Baseline would lack the location/inventory guardrails the structured state provides.
2. How much useful memory saved? — 3 AUTO Current State slots + 1 REVIEW relationship per chapter.
3. How much useless memory? — Physical-condition "受伤" repeats every chapter (mock determinism); low value but not harmful.
4. Does REVIEW burden the author? — Minor; only relationship candidate needs a click.
5. Does Writer use Memory? — Structurally yes (passed in request); prose usage needs real LLM to confirm.
6. Does Planner reduce planning work? — Yes (one direction → 5-chapter plan).
7. Manual corrections needed? — 0 required.
8. Most valuable memory type? — Current State (location/inventory) — directly queryable.
9. Least valuable? — Repeated physical_condition (mock redundancy).
10. Biggest real failure mode? — MOCK writer cannot demonstrate prose-level memory benefit; needs real LLM for full validation.

## v0.1 Known Issues (TASK-052)
- **KI-01**: MOCK LLM writer is deterministic and self-contained; prose-level memory benefit (Baseline vs Memory) cannot be demonstrated without `LLM_API_KEY`. Acceptance §30 permits this.
- **KI-02**: STORY_MEMORY (foreshadowing) candidates are not produced by the mock extractor for the default generated text (no 黑袍/玉佩-停顿 trigger). Real LLM extraction would populate them. Not a code defect.
- **KI-03**: Each chapter repeats identical AUTO candidates (location/inventory/physical_condition) due to mock determinism — real LLM would vary; dedupe-by-slot already prevents duplicate Current State rows.
- **KI-04**: Backend fat-jar (`target/*.jar`) was locked by an external handle in this environment; a classpath launch (`java -cp "target/classes;build-deps/*"`) or manual reassembly was used. Build via `mvn package` is the normal path and should work in a clean environment.
- **KI-05**: Java 17 (Corretto) used vs spec Java 21 — functional, no behavior impact for v0.1.
- **KI-06**: Vite `dist/` may need manual `rm -rf` before `vite build` in this sandbox (safe-delete shim); `rm -rf dist` first resolves it.
- **KI-07**: Backend jar must be launched with explicit `--server.port=8080` (sandbox injects a random port otherwise).

## Acceptance Evidence (§33) — locations
- Acceptance Story: `ACCEPTANCE_TESTS.md` §5/§6
- 5 continuous chapters: Story 200, Stage 122 (DB `chapter` rows)
- Stage Plan: `chapter_plan` rows for stage 122
- Story Constraints: `story_constraint` rows
- Current State example: `current_state` rows (location/inventory/physical_condition)
- Story Memory example: `story_memory` rows (populated on real-LLM run)
- REVIEW example: `memory_candidate` rows (RELATIONSHIP, processing_status=PENDING/APPLIED)
- Source Evidence example: `memory_candidate.evidence` + `source_chapter_id`
- Story Query example: "当前位置：禁书区最深处。" / "当前持有：生锈的铜钥匙。"
- Planner Suggestions: 冲突型 / 成长型 / 悬疑型
- Baseline vs Memory: this file (§Experiment N1)
- Known Issues: this file (§v0.1 Known Issues)
