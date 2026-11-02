"""Count executed JUnit XML testcases, never source annotations or planned tests."""
import argparse
import json
import os
from pathlib import Path
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument("--require-complete", action="store_true")
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
groups = {
    "jvm": root / "app/build/test-results/testDebugUnitTest",
    "instrumented": root / "app/build/outputs/androidTest-results/connected",
}
summary = {}
for name, directory in groups.items():
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    files = sorted(directory.rglob("TEST-*.xml"))
    for file in files:
        tree = ET.parse(file)
        for case in tree.iter("testcase"):
            totals["tests"] += 1
            for status in ("failure", "error", "skipped"):
                if case.find(status) is not None:
                    totals[{"failure": "failures", "error": "errors", "skipped": "skipped"}[status]] += 1
    totals["passed"] = totals["tests"] - totals["failures"] - totals["errors"] - totals["skipped"]
    summary[name] = {**totals, "xml_files": len(files)}
screenshots = sorted(p.name for p in (root / "evidence/screenshots").glob("*.png"))
output = root / "evidence"
output.mkdir(exist_ok=True)
run_url = None
if os.getenv("GITHUB_RUN_ID"):
    run_url = os.getenv("GITHUB_SERVER_URL", "https://github.com") + "/" + os.environ["GITHUB_REPOSITORY"] + "/actions/runs/" + os.environ["GITHUB_RUN_ID"]
payload = {"suites": summary, "screenshots": screenshots, "run_url": run_url}
(output / "test-summary.json").write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
lines = ["| Suite | Tests | Passed | Failures | Errors | Skipped |", "|---|---:|---:|---:|---:|---:|"]
for name, counts in summary.items():
    lines.append(f"| {name} | {counts['tests']} | {counts['passed']} | {counts['failures']} | {counts['errors']} | {counts['skipped']} |")
report = "\n".join(lines) + f"\n\nScreenshots: {', '.join(screenshots) or 'none'}\n"
(output / "test-summary.md").write_text(report, encoding="utf-8")
print(report)
if os.getenv("GITHUB_STEP_SUMMARY"):
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as stream:
        stream.write(report)
if args.require_complete:
    complete = all(c["tests"] > 0 and not (c["failures"] or c["errors"] or c["skipped"]) for c in summary.values())
    complete = complete and screenshots == ["overview.png", "spending.png", "transfer.png"]
    raise SystemExit(0 if complete else 1)
