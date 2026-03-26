from __future__ import annotations

import pathlib
import re
import sys


ROOT = pathlib.Path(__file__).resolve().parent.parent
SEARCH_DIRS = [ROOT / "lib", ROOT / "test"]
STRING_RE = re.compile(r"""('(?:[^'\\]|\\.)*'|"(?:[^"\\]|\\.)*")""")
SUSPICIOUS_PATTERNS = ("???", "\uFFFD")


def main() -> int:
    issues: list[str] = []

    for base_dir in SEARCH_DIRS:
        if not base_dir.exists():
            continue

        for path in base_dir.rglob("*.dart"):
            text = path.read_text(encoding="utf-8")
            for line_number, line in enumerate(text.splitlines(), start=1):
                for match in STRING_RE.finditer(line):
                    literal = match.group(0)
                    if any(pattern in literal for pattern in SUSPICIOUS_PATTERNS):
                        issues.append(f"{path}:{line_number}: {literal}")

    if issues:
        print("Suspicious text literals found:")
        for issue in issues:
            print(issue)
        return 1

    print("No suspicious text literals found.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
