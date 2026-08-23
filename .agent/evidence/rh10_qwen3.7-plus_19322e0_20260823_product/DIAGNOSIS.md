# RH-10 Product-wired Run Diagnosis

This complete run passed AC-101/103/104/106/109/114 and failed AC-105.

The extractor repair worked: the current Manual Edit sentence was persisted as
`TRANSIENT_DETAIL / importance=1 / scope=CHAPTER / IGNORE`, and the candidate
was not selected as StoryMemory. The failure was nevertheless genuine:

- Chapter 3 mentioned baked-bread smell once as scene texture.
- Chapter 4 invented half a rye loaf as a key disappearance clue and reasoned
  about its cut three times.
- Chapter 5 continued that clue as an explanation for the victim's state.

The ignored detail bypassed selective Memory through the raw latest-chapter
ending in `recentContext`. Therefore this run remains FAILED and cannot be used
for the release verdict. Its full API traces, persisted Story snapshots and all
six passing semantic results are retained as regression evidence.
