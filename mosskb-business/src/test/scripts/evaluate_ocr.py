"""Offline evaluation of saved OCR responses; no keys or network required."""
import argparse
import difflib
import html
import json
import re
import unicodedata
from pathlib import Path


def clean(text):
    text = re.sub(r"<\|ref\|>.*?<\|/det\|>", "", text, flags=re.S)
    text = re.sub(r"\b(?:title|text|ref_text|list|table|image|figure|formula|equation|header|footer|page_number|caption)\s*\[[\d, .]+\]", "", text)
    text = re.sub(r"!?\[[^\]]*\]\([^)]*\)", "", text)
    text = re.sub(r"<[^>]*>", "", text)
    text = re.sub(r"\\[A-Za-z]+", "", text)
    text = re.sub(r"(?m)^\s*>\s*Page\s+\d+\s*$", "", text)
    text = re.sub(r"[—–-]\s*\d+\s*[—–-]", "", text)
    return html.unescape(text)


def normalize(text):
    return "".join(c for c in unicodedata.normalize("NFKC", clean(text)) if c.isalnum())


def levenshtein(a, b):
    n = 0
    while n < min(len(a), len(b)) and a[n] == b[n]:
        n += 1
    a, b = a[n:], b[n:]
    n = 0
    while n < min(len(a), len(b)) and a[-n-1] == b[-n-1]:
        n += 1
    if n:
        a, b = a[:-n], b[:-n]
    if len(a) > len(b):
        a, b = b, a
    row = list(range(len(a)+1))
    for j, cb in enumerate(b, 1):
        new = [j]
        for i, ca in enumerate(a, 1):
            new.append(min(new[-1]+1, row[i]+1, row[i-1]+(ca != cb)))
        row = new
    return row[-1]


def aligned_distance(reference, candidate):
    """Exact semiglobal edit distance; no penalty for surrounding page text."""
    if reference in candidate:
        return 0, reference
    row = [0] * (len(candidate) + 1)
    starts = list(range(len(candidate) + 1))
    for i, ca in enumerate(reference, 1):
        new, new_starts = [i], [0]
        for j, cb in enumerate(candidate, 1):
            options = [(row[j-1] + (ca != cb), starts[j-1]),
                       (row[j] + 1, starts[j]), (new[-1] + 1, new_starts[-1])]
            cost, start = min(options, key=lambda x: x[0])
            new.append(cost)
            new_starts.append(start)
        row, starts = new, new_starts
    end = min(range(len(row)), key=row.__getitem__)
    return row[end], candidate[starts[end]:end]


def read_model(folder):
    raw = folder / "task.raw.json"
    if raw.exists():
        task = json.loads(raw.read_text(encoding="utf-8"))
        if task.get("status") != "success":
            return None
        out = task.get("output") or {}
        # For this PDF, visual inspection confirms ordered page segments.
        # Provider indices can restart in each batch; never globally sort them.
        if "segments" in out:
            pages = {i+1: s.get("content", "") for i, s in enumerate(out["segments"])}
        else:
            pages = {p.get("page_index", i)+1: p.get("text_result", "") for i, p in enumerate(out.get("pages", []))}
        created, completed = task.get("created_at"), task.get("completed_at")
        seconds = (completed-created)/1000 if created and completed else None
        return pages, {"providerTotalSeconds": seconds, "priceCny": task.get("price"), "status": task.get("status")}
    files = sorted(folder.glob("page-*.md"))
    if files:
        pages = {int(f.stem.split("-")[1]): f.read_text(encoding="utf-8") for f in files}
        states = [json.loads(f.read_text(encoding="utf-8")) for f in folder.glob("page-*.state.json")]
        return pages, {"requestSecondsSum": sum(s.get("elapsedMs", 0) for s in states)/1000, "priceCny": None, "status": "success" if len(pages) == 35 else "partial"}
    return None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--runs", nargs="+", required=True, type=Path)
    parser.add_argument("--reference", required=True, type=Path)
    parser.add_argument("--source-text", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    reference = json.loads(args.reference.read_text(encoding="utf-8"))
    baseline = {int(k): v for k, v in reference["pages"].items()}
    for page in range(28, 33):
        baseline[page] = (args.source_text / f"source-{page:02d}.txt").read_text(encoding="utf-8")
    evaluation = {"method": reference["description"], "samplePages": sorted(baseline), "models": {}}
    args.output.mkdir(parents=True, exist_ok=True)
    for run in args.runs:
        for folder in sorted(p for p in run.iterdir() if p.is_dir()):
            data = read_model(folder)
            if not data:
                continue
            pages, metrics = data
            model = folder.name
            metrics["returnedPageEntries"] = len(pages)
            metrics["nonemptyPageEntries"] = sum(bool(normalize(s)) for s in pages.values())
            metrics["sampleDetails"] = []
            errors = chars = 0
            diffs = []
            for p, ground in baseline.items():
                a, b = normalize(ground), normalize(pages.get(p, ""))
                # Some parsers join a paragraph split over adjacent physical pages.
                # Align within a three-page window rather than counting reflow as loss.
                window = normalize("".join(pages.get(q, "") for q in range(max(1,p-1),p+2)))
                distance, b = aligned_distance(a, window)
                errors += distance
                chars += len(a)
                metrics["sampleDetails"].append({"page": p, "referenceChars": len(a), "editDistance": distance, "cer": distance/len(a)})
                for tag, i, j, k, l in difflib.SequenceMatcher(None, a, b, autojunk=False).get_opcodes():
                    if tag != "equal":
                        diffs.append({"page": p, "kind": tag, "reference": a[i:j], "actual": b[k:l], "context": a[max(0,i-12):min(len(a),j+12)]})
            metrics["sampleReferenceChars"] = chars
            metrics["sampleEditDistance"] = errors
            metrics["sampleCER"] = errors/chars
            metrics["probes"] = [{**probe, "matched": normalize(probe["text"]) in normalize(pages.get(probe["page"], ""))} for probe in reference["probes"]]
            metrics["probeHits"] = sum(p["matched"] for p in metrics["probes"])
            metrics["probeTotal"] = len(metrics["probes"])
            metrics["tables"] = {}
            for p in [27, 33, 34]:
                text = pages.get(p, "")
                metrics["tables"][p] = {"htmlTable": "<table" in text.lower(), "htmlCells": len(re.findall(r"<t[dh]\b", text, re.I)), "rowspan": "rowspan" in text, "colspan": "colspan" in text, "normalizedChars": len(normalize(text))}
            evaluation["models"][model] = metrics
            (args.output / f"{model}-diffs.json").write_text(json.dumps(diffs, ensure_ascii=False, indent=2), encoding="utf-8")
            perpage = "\n\n".join(f"## PDF physical page {p}\n\n{text}" for p, text in pages.items())
            (args.output / f"{model}-pages.md").write_text(perpage, encoding="utf-8")
    (args.output / "evaluation.json").write_text(json.dumps(evaluation, ensure_ascii=False, indent=2), encoding="utf-8")
    for model, m in evaluation["models"].items():
        print(f"{model}: CER={m['sampleCER']:.3%} ({m['sampleEditDistance']}/{m['sampleReferenceChars']}), probes={m['probeHits']}/{m['probeTotal']}, pages={m['nonemptyPageEntries']}/{m['returnedPageEntries']}")


if __name__ == "__main__":
    main()
