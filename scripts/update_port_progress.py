"""Refresh the Fabric port progress bar from the README feature tables."""

import re
from pathlib import Path
from urllib.parse import urlencode


README = Path(__file__).resolve().parents[1] / "README.md"
START = "<!-- port-progress:start -->"
END = "<!-- port-progress:end -->"
LEGACY_FEATURE_ROW = re.compile(r"^\|.+\|[ \t]*X[ \t]*\|([^|]*)\|[ \t]*$", re.MULTILINE)


def update_progress(readme: str) -> str:
    _, heading, features = readme.partition("\n## Features\n")
    if not heading:
        raise ValueError("README.md is missing the Features section.")

    features = features.split("\n## ", 1)[0]
    statuses = LEGACY_FEATURE_ROW.findall(features)
    if not statuses:
        raise ValueError("The Features section has no rows marked X in 1.8.9.")

    total = len(statuses)
    ported = sum(status.strip() == "X" for status in statuses)
    label = f"1.21 port progress ({ported / total:.1%})"
    percentage = int((ported / total) * 100)
    params = urlencode({
        "scale": 100,
        "width": 350,
        #"color": "338b33",
        "progress_color": "338b33"
    })
    summary = f"![{label}](https://progress-bar.xyz/{percentage}/?{params}) `{ported}/{total} features`"

    before, start, rest = readme.partition(START)
    _, end, after = rest.partition(END)
    if not start or not end:
        raise ValueError("README.md is missing the port progress markers.")
    return f"{before}{start}\n\n{summary}\n\n{end}{after}"


def main() -> None:
    contents = README.read_bytes()
    newline = "\r\n" if b"\r\n" in contents else "\n"
    readme = contents.decode("utf-8").replace("\r\n", "\n")
    updated = update_progress(readme).replace("\n", newline).encode("utf-8")
    if updated != contents:
        README.write_bytes(updated)


if __name__ == "__main__":
    main()
