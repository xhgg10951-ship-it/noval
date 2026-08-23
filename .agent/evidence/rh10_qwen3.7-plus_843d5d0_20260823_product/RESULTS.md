# RH-10 Product-wired Failed Run

Run ID: `rh10_qwen3.7-plus_843d5d0_20260823_product`

Product/prompt commit: `843d5d0`

Provider: `qwen3.7-plus`, `mock_llm=false`

| Acceptance | Result | Observed product behavior |
|---|---|---|
| AC-101 | PASS | Persisted continuation plans used logical orders 3–5 and did not restart the story. |
| AC-103 | PASS | Five consecutive persisted chapters met the frozen length and no-padding gate. |
| AC-104 | PASS | Guild registration completed without identity disclosure. |
| AC-105 | FAIL | The appended ordinary-bread sentence was present in current content but absent from all 23 extracted candidates. |
| AC-109 | FAIL | Polish omitted `铁剑` and the exact `冒险者公会` location, and introduced modern clothes/a trainee badge. |
| AC-114 | PASS | At current=5/target=600/Arc=1–60, orders 6–10 stayed on local survival/investigation progress. |
| AC-106 | BLOCKED | Stop on a PAUSED STEP Job returned PAUSED forever because no worker remained to consume `stopRequested`. |

The runner was interrupted after the PAUSED Stop blocker was proven. All 388
raw API traces up to that point are retained in this directory. This run is
failure evidence only and cannot be used for the release verdict.
