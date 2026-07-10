import os
import sys

out_file = r"F:\open\export_for_review\ALL_SOURCE_CODE.md"
root = r"F:\open\export_for_review"

extensions = {'.java', '.json', '.toml', '.gradle', '.properties', '.md', '.txt', '.mcmeta', '.mixins.json', '.lang'}

files = []
for dirpath, dirnames, filenames in os.walk(root):
    # Skip node_modules
    if 'node_modules' in dirpath:
        continue
    for fname in filenames:
        ext = os.path.splitext(fname)[1]
        if ext in extensions:
            full = os.path.join(dirpath, fname)
            if 'ALL_SOURCE_CODE' not in full:
                files.append(full)

files.sort()

with open(out_file, 'w', encoding='utf-8') as out:
    for f in files:
        rel = os.path.relpath(f, root)
        ext = os.path.splitext(f)[1][1:]
        if not ext:
            ext = 'txt'
        out.write(f'\n```{ext}\n// {rel}\n')
        with open(f, 'r', encoding='utf-8') as inp:
            out.write(inp.read())
        out.write('\n```\n')

size_mb = round(os.path.getsize(out_file) / (1024*1024), 2)
print(f"Done. Size: {size_mb} MB")