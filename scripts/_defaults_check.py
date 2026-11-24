"""Scratch (read-only): compare config defaults in code with the first backticked value in doc table rows.

Rows looked at: `| Key | `value` | ...` (a pipe table whose first cell equals an @Config.Name). Prints mismatches.
"""
import glob
import os
import re

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
src = open(root + '/src/main/java/com/apocollis/aqtweaks/ArcanaQuestTweaksConfig.java', encoding='utf-8').read()

defaults = {}
for m in re.finditer(r'@Config\.Name\("([^"]+)"\)(.*?)(?=@Config\.Name|$)', src, re.S):
    name, body = m.group(1), m.group(2)
    d = re.search(r'public (?:static )?(?:final )?([A-Za-z\[\]<>, ]+?) \w+ = ([^;]+);', body)
    if d:
        defaults.setdefault(name, set()).add((d.group(1).strip(), d.group(2).strip()))


def norm(v):
    v = v.strip().strip('`').strip()
    v = re.sub(r'(?<=\d)[DdFfLl]$', '', v)
    v = v.replace('"', '').replace('_', '')
    v = v.lower()
    try:
        f = float(v)
        return repr(round(f, 6)).rstrip('0').rstrip('.') if '.' in repr(f) else repr(f)
    except ValueError:
        return v


issues = []
for f in sorted(glob.glob(root + '/docs/*.md')):
    base = os.path.basename(f)
    if base in ('changelog.md',):
        continue
    for ln, line in enumerate(open(f, encoding='utf-8', errors='replace').read().split('\n'), 1):
        if not line.startswith('|'):
            continue
        cells = [c.strip() for c in line.strip().strip('|').split('|')]
        if len(cells) < 2:
            continue
        key = cells[0].strip('`')
        if key not in defaults:
            continue
        doc = cells[1].strip('`')
        if doc.startswith('(') or doc in ('', '-'):
            continue
        outs = {norm(v[1]) for v in defaults[key] if v[0] in ('int', 'double', 'float', 'boolean', 'long', 'String')}
        if not outs:
            continue
        nd = norm(doc.split(' ')[0] if doc.split(' ')[0] else doc)
        nd_full = norm(doc)
        if nd not in outs and nd_full not in outs:
            issues.append('%s:%d  %s  doc=%s  code=%s' % (base, ln, key, doc, sorted(outs)))
print('\n'.join(issues))
print(len(issues), 'possible mismatches')
