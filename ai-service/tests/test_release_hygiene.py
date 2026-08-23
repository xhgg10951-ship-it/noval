from pathlib import Path


REPO = Path(__file__).resolve().parents[2]


def read(relative: str) -> str:
    return (REPO / relative).read_text(encoding="utf-8")


def test_current_release_documents_do_not_reuse_withdrawn_acceptance():
    readme = read("README.md")
    metrics = read(".agent/evidence/ACCEPTANCE_METRICS.md")
    fixture = read(".agent/evidence/ACCEPTANCE_FIXTURE.md")

    assert "Current Release Verdict:  NOT ACCEPTED" in readme
    assert "Current Real-LLM Gate:    NOT RUN" in readme
    assert "Current release verdict:  NOT ACCEPTED" in metrics
    assert "Current RH-10 status:      NOT RUN" in metrics
    assert "Migrations:         V1..V16" in fixture
    assert "Release verdict:    NOT ACCEPTED" in fixture
    assert "Run ID:             PENDING RH-10" in fixture


def test_runbook_and_ci_apply_every_current_migration():
    runbook = read("RUN.md")
    workflow = read(".github/workflows/ci.yml")

    assert "`V1` through `V16`" in runbook
    assert "1..16 | ForEach-Object" in runbook
    assert "seq 1 16" in runbook
    assert "Apply migrations V1 through V16" in workflow
    assert "seq 1 16" in workflow
