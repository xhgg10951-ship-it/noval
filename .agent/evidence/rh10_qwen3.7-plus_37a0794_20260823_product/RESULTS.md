# RH-10 Invalid-fixture Run

This product-wired run passed AC-101/103/104/109 before it was aborted at the
AC-105 investigation.

The Context repair correctly removed the grounded ignored-candidate evidence.
However, the runner's chapter 4 and 5 goals literally contained “不得围绕早餐或
面包展开”. The frozen AC-105 requires three **unrelated** ChapterSpecs, so the
fixture itself placed the low-value term into the Writer's high-priority goal
and was invalid. This run cannot pass or fail the release gate.

The fixture was corrected to use only guild-record, route and supply goals with
no bread/breakfast term. Raw traces are retained for audit; no output from this
run is reused by the next complete run.
