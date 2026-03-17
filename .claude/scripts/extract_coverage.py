#!/usr/bin/env python3
"""
JaCoCo XML から指定クラスの未カバーブランチのみを抽出する。
Usage: python3 extract_coverage.py <path/to/ClassName.java>
"""
import xml.etree.ElementTree as ET
import sys
import os
import json

if len(sys.argv) < 2:
    print(json.dumps({"error": "Usage: extract_coverage.py <path/to/ClassName.java>"}))
    sys.exit(1)

target_path = sys.argv[1]
class_name = os.path.basename(target_path)

xml_path = "target/site/jacoco/jacoco.xml"
if not os.path.exists(xml_path):
    print(json.dumps({"error": "jacoco.xml not found. Run: mvn test jacoco:report"}))
    sys.exit(1)

tree = ET.parse(xml_path)
root = tree.getroot()

result = {"class": class_name, "uncovered_lines": [], "branch_summary": {}}

for sf in root.findall(f'.//sourcefile[@name="{class_name}"]'):
    for line in sf.findall("line"):
        mb = int(line.get("mb", 0))
        cb = int(line.get("cb", 0))
        if mb > 0:
            result["uncovered_lines"].append({
                "line": int(line.get("nr")),
                "missed_branches": mb,
                "covered_branches": cb
            })
    for counter in sf.findall("counter[@type='BRANCH']"):
        missed = int(counter.get("missed", 0))
        covered = int(counter.get("covered", 0))
        total = missed + covered
        result["branch_summary"] = {
            "missed": missed,
            "covered": covered,
            "total": total,
            "pct": round(covered / total * 100, 1) if total > 0 else 100.0
        }

print(json.dumps(result, indent=2))
