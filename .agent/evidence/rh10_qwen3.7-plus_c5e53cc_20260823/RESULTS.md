# RH-10 qwen3.7-plus Results

Run ID: `rh10_qwen3.7-plus_c5e53cc_20260823`

Product/prompt commit: `c5e53cc`

Provider mode: `qwen3.7-plus`, `mock_llm=false`

| Acceptance | Result | Evidence / measured result |
|---|---|---|
| AC-101 Continuation | PASS | Planner starts at chapter 4; forbidden restart hits 0; 22.55 s |
| AC-103 Length | PASS | 2896/2828/3155/2874/2843 chars; 5/5 in band; 173.13 s |
| AC-103 No Padding | PASS | 0 exact duplicate long sentences; max internal similarity 0.458; max cross-chapter 0.702 |
| AC-104 Chapter Goal | PASS | enter/register/test preparation 3/3; hidden-identity violations 0; 3241 chars; 43.18 s |
| AC-105 Low-value Detail | PASS | exact TRANSIENT_DETAIL/1/CHAPTER/IGNORE; later bread repetition 0/3; 72.65 s |
| AC-106 Replan Semantic | PASS | three continued plans; four continuation terms; forbidden repeat hits 0; 21.40 s |
| AC-109 Polish | PASS | seven fact/style/ending checks passed; final run 3.66 s |
| AC-114 600-chapter Pace | PASS | five near-term plans; endgame-pattern hits 0; 23.86 s |

AC-104 had one harness false negative before the passing run: the generated
chapter opened with “推开那扇厚重的橡木大门” and proceeded inside the guild,
but the initial equivalent-expression list only recognized 到达/进入/走进.
The failed raw response is retained. Adding 推开 to the entry-expression group
made the check match the frozen semantic requirement; no product prompt or
response was edited.

AC-109 also exposed an equivalent-expression gap while converting its console
checks to structured evidence: a response preserved the discovery as “洞穴、抓痕、
染血衣角”, while the initial checker only recognized 山洞/痕迹/线索/微光. The
checker was expanded to those direct clue synonyms; no product prompt or output
was edited.

The AI service was shut down after all evidence files were flushed.
