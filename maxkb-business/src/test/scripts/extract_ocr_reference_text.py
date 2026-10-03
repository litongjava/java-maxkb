"""Read the original PDF text layer for evaluation; never modifies the PDF."""
import argparse
from pathlib import Path
from pypdf import PdfReader

parser = argparse.ArgumentParser()
parser.add_argument("pdf", type=Path)
parser.add_argument("output", type=Path)
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=True)
reader = PdfReader(args.pdf)
for index, page in enumerate(reader.pages, 1):
    text = page.extract_text() or ""
    (args.output / f"source-{index:02d}.txt").write_text(text, encoding="utf-8")
    print(f"page {index}: {len(text)} text-layer characters")
