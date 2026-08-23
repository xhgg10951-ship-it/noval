from pathlib import Path
import runpy


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


def test_rh10_low_value_followups_are_truly_unrelated_specs():
    suite = runpy.run_path(str(REPO / "scripts/rh10_product_suite.py"))
    goals = suite["AC105_UNRELATED_GOALS"]
    specs = suite["AC105_UNRELATED_SPECS"]

    assert len(goals) == 3
    assert len(specs) == 3
    assert all("面包" not in goal and "早餐" not in goal for goal in goals)
    assert all(
        "面包" not in str(value) and "早餐" not in str(value)
        for spec in specs for value in spec.values()
    )


def test_rh10_fixture_removes_naturally_generated_bread_before_inserting_test_detail():
    suite = runpy.run_path(str(REPO / "scripts/rh10_product_suite.py"))
    clean = suite["remove_preexisting_bread"](
        "林夜整理行李。\n艾琳咬了一口黑面包。\n两人准备出门。")

    assert "面包" not in clean
    assert "林夜整理行李" in clean
    assert "两人准备出门" in clean
