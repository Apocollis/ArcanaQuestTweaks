"""Scratch (read-only): doc mentions like `Class.method` or `Class#method` where the class is ours but the file has no such method/field."""
import glob
import os
import re

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
files = {}
for f in glob.glob(root + '/src/main/java/**/*.java', recursive=True):
    files[os.path.basename(f)[:-5]] = open(f, encoding='utf-8', errors='replace').read()

seen = set()
for d in sorted(glob.glob(root + '/docs/*.md')):
    base = os.path.basename(d)
    if base == 'changelog.md':
        continue
    text = open(d, encoding='utf-8', errors='replace').read()
    for m in re.finditer(r'`([A-Z][A-Za-z0-9]+)[.#]([a-z][A-Za-z0-9_]*)(?:\([^`]*\))?`', text):
        cls, member = m.group(1), m.group(2)
        if cls not in files or member in ('java', 'json', 'md'):
            continue
        src = files[cls]
        if re.search(r'\b' + re.escape(member) + r'\b', src):
            continue
        key = (base, cls, member)
        if key in seen:
            continue
        seen.add(key)
        print('%s: %s.%s not found in %s.java' % (base, cls, member, cls))
print(len(seen), 'unresolved member mentions')
