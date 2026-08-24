from pathlib import Path
import runpy


REPO = Path(__file__).resolve().parents[2]


def read(relative: str) -> str:
    return (REPO / relative).read_text(encoding="utf-8")


def test_current_release_documents_record_verified_acceptance():
    readme = read("README.md")
    metrics = read(".agent/evidence/ACCEPTANCE_METRICS.md")
    fixture = read(".agent/evidence/ACCEPTANCE_FIXTURE.md")

    assert "Current Release Verdict:  ACCEPTED" in readme
    assert "Current Real-LLM Gate:    PASSED" in readme
    assert "Hosted CI:                PASSED — run 32676289823" in readme
    assert "Current release verdict:  ACCEPTED" in metrics
    assert "Current RH-10 status: PASSED" in metrics
    assert "RH-10 evidence commit: `3aafee0`" in metrics
    assert "Migrations:         V1..V16" in fixture
    assert "Release verdict:    ACCEPTED" in fixture
    assert (
        "Run ID:             "
        "rh10_qwen3.7-plus_4696b4f_focus_20260824_product"
    ) in fixture

    required_metrics = [
        "Prompt version",
        "Story IDs",
        "Continuation failures",
        "Duplicate active Memory",
        "Revision operations",
        "Memory extraction failures",
        "Average generation latency",
    ]
    assert all(metric in metrics for metric in required_metrics)


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


def test_rh10_low_value_check_allows_incidental_world_detail_but_rejects_focus():
    suite = runpy.run_path(str(REPO / "scripts/rh10_product_suite.py"))
    check = suite["low_value_bread_is_isolated"]
    incidental = check([
        {"content": "林夜穿过长街。" * 300 + "面包房传来焦香。"},
        {"content": "林夜在公会核对失踪者档案。" * 200},
        {"content": "林夜购买药剂和绳索。" * 200 + "他顺手补充了黑麦面包。"},
    ])
    focused = check([
        {"content": "普通面包成为调查重点。" * 100},
        {"content": "林夜检查面包线索。" * 100},
        {"content": "艾琳继续追查面包来源。" * 100},
    ])

    assert incidental["passed"] is True
    assert focused["passed"] is False
