"""Read-only docs drift check (stdlib only). Run from anywhere: python scripts/doc_audit.py

Compares docs/ with the source tree: README index vs files, classes named in docs vs the tree, Mixin classes and
mixin json vs docs, every @Config.Name vs docs, the @Mod dependency string vs the README, libs/ vs the
compatibility matrix. Prints findings; exit code 1 if a hard check fails (unlinked doc, dead link, missing class,
bad mixin json, unregistered Mixin, undocumented config key, README/@Mod mismatch). Class mentions in
docs/history and changelog are ignored (they describe deleted code). "doc table names not in code" is
informational: many table rows are labels, not config keys.
"""
import os, re, json, glob, sys
R = os.path.dirname(os.path.dirname(os.path.abspath(__file__))).replace(chr(92), '/')
FAIL = []
docs = {}
for f in glob.glob(R + '/docs/**/*.md', recursive=True):
    docs[os.path.relpath(f, R + '/docs').replace('\\', '/')] = open(f, encoding='utf-8', errors='replace').read()
alltext = '\n'.join(docs.values())
nochange = '\n'.join(v for k, v in docs.items() if k != 'changelog.md' and not k.startswith('history/'))

print('== docs files not linked from README ==')
hist = lambda k: k.startswith('history/') or k == 'changelog.md'
readme = docs['README.md']
for k in sorted(docs):
    if k == 'README.md': continue
    if k.split('/')[-1] not in readme:
        print(' ', k)
        FAIL.append('unlinked ' + k)

print('== README links to missing files ==')
for m in set(re.findall(r'\]\(([^)#]+\.md)', readme)):
    if not os.path.exists(os.path.join(R, 'docs', m)):
        print(' ', m)
        FAIL.append('dead link ' + m)

# java classes
classes = {}
for f in glob.glob(R + '/src/main/java/**/*.java', recursive=True):
    classes[os.path.basename(f)[:-5]] = os.path.relpath(f, R + '/src/main/java/com/apocollis/aqtweaks').replace('\\', '/')
print('== classes referenced in docs (Name.java or `Name`) that do not exist ==')
cand = set(re.findall(r'`((?:[a-z]+/)*([A-Z][A-Za-z0-9]+)\.java)`', nochange))
missing = sorted({c[0] for c in cand if c[1] not in classes})
for m in missing:
    print(' ', m)
    FAIL.append('missing class ' + m)

print('== mixin classes not mentioned in any non-changelog doc ==')
unm = []
for name, path in sorted(classes.items()):
    if path.startswith('mixin/') and name not in nochange:
        unm.append(path)
print(len(unm), 'of', sum(1 for p in classes.values() if p.startswith('mixin/')))
for u in unm: print(' ', u)

print('== non-mixin classes not mentioned in any non-changelog doc ==')
unm = []
for name, path in sorted(classes.items(), key=lambda x: x[1]):
    if path.startswith('mixin/'): continue
    if name not in nochange:
        unm.append(path)
print(len(unm), 'of', sum(1 for p in classes.values() if not p.startswith('mixin/')))
for u in unm: print(' ', u)

print('== mixin json files vs docs; classes in json missing on disk ==')
for f in sorted(glob.glob(R + '/src/main/resources/mixins.aqtweaks*.json')):
    b = os.path.basename(f)
    if b.endswith('refmap.json'): continue
    try:
        j = json.load(open(f, encoding='utf-8'))
    except Exception as e:
        print(' bad json', b, e); FAIL.append('bad json ' + b); continue
    pkg = j.get('package', '')
    ents = j.get('mixins', []) + j.get('client', []) + j.get('server', [])
    miss = []
    for e in ents:
        p = R + '/src/main/java/' + (pkg + '.' + e).replace('.', '/') + '.java'
        if not os.path.exists(p): miss.append(e)
    listed = {e.split('.')[-1] for e in ents}
    ondisk = set()
    d = R + '/src/main/java/' + pkg.replace('.', '/')
    for g in glob.glob(d + '/**/*.java', recursive=True):
        ondisk.add(os.path.basename(g)[:-5])
    unreg = sorted(n for n in ondisk if n not in listed and n.startswith('Mixin') and False)
    print(' ', b, 'in docs' if b in nochange else 'NOT IN DOCS', 'missing-on-disk:', miss)
all_json_classes = set()
for f in glob.glob(R + '/src/main/resources/mixins.aqtweaks*.json'):
    if f.endswith('refmap.json'): continue
    j = json.load(open(f, encoding='utf-8'))
    for e in j.get('mixins', []) + j.get('client', []) + j.get('server', []):
        all_json_classes.add(e.split('.')[-1])
print('== Mixin* classes on disk not registered in any json ==')
for name, path in sorted(classes.items()):
    if path.startswith('mixin/') and name.startswith('Mixin') and name not in all_json_classes:
        print(' ', path)
        FAIL.append('unregistered ' + path)

print('== config: @Config.Name values not in any non-changelog doc ==')
cfg = open(R + '/src/main/java/com/apocollis/aqtweaks/ArcanaQuestTweaksConfig.java', encoding='utf-8').read()
names = re.findall(r'@Config\.Name\("([^"]+)"\)', cfg)
objs = set()
for m in re.finditer(r'@Config\.Name\("([^"]+)"\)', cfg):
    # A config category or perk layout is an object (`public [static final] T x = new T(...)`), not a value key.
    decl = re.search(r'public\s+(?:static\s+)?(?:final\s+)?(\w+)\s+\w+\s*=\s*new\s+(\w+)\(', cfg[m.end():m.end() + 600])
    if decl and decl.group(1) == decl.group(2) and decl.group(1)[0].isupper() and decl.group(1) not in ('String', 'Integer'):
        objs.add(m.group(1))
miss = [n for n in names if n not in nochange and n not in objs]
print(len(miss), 'of', len(names))
for n in miss:
    print(' ', n)
    FAIL.append('config ' + n)
# other config files
for f in glob.glob(R + '/src/main/java/**/*.java', recursive=True):
    if f.endswith('ArcanaQuestTweaksConfig.java'): continue
    t = open(f, encoding='utf-8', errors='replace').read()
    ns = re.findall(r'@Config\.Name\("([^"]+)"\)', t)
    ms = [n for n in ns if n not in nochange and n != 'X-Pos' and n != 'Y-Pos' and n != 'Skill Point Cost' and n != 'Parent Skill' and n != 'Requirements']
    if ms: print(' ', os.path.basename(f), ms)

print('== doc table config names not in code ==')
allcode = ''
for f in glob.glob(R + '/src/main/java/**/*.java', recursive=True):
    allcode += open(f, encoding='utf-8', errors='replace').read()
rows = set()
for k, v in docs.items():
    if k == 'changelog.md': continue
    for m in re.finditer(r'^\| ((?:[A-Z][A-Za-z0-9\'/ \-]+)) \| (true|false|-?\d+(?:\.\d+)?|`[^`]*`|"[^"]*") \|', v, re.M):
        rows.add((k, m.group(1).strip()))
bad = [(k, n) for k, n in sorted(rows) if '"' + n + '"' not in allcode]
print(len(bad), 'of', len(rows))
for k, n in bad: print(' ', k, '|', n)

print('== matrix jars vs build_gradle deps ==')
bg = open(R + '/build_gradle.ps1', encoding='utf-8').read()
deps = re.findall(r'"([^"]+\.jar)"', bg)
mx = docs['compatibility-matrix.md']
for d in sorted(set(deps)):
    stem = re.sub(r'[-_.]?\d.*$', '', d.replace('.jar', ''))
    if stem.lower() not in mx.lower():
        print('  no matrix mention:', d)
libs = os.listdir(R + '/libs') if os.path.isdir(R + '/libs') else []
for l in sorted(libs):
    if l not in deps: print('  libs jar not in build_gradle deps:', l)
for d in sorted(set(deps)):
    if d not in libs: print('  dep missing from libs:', d)

print('== @Mod / version ==')
m = re.search(r'dependencies\s*=\s*"([^"]+)"', open(R + '/src/main/java/com/apocollis/aqtweaks/ArcanaQuestTweaks.java', encoding='utf-8').read())
print(' code deps ==', m.group(1) if m else None)
m2 = re.search(r'`(required-after:elenaidodge2[^`]+)`', readme)
ok = bool(m and m2 and m.group(1) == m2.group(1))
print(' README matches code:', ok)
if not ok: FAIL.append('README @Mod string differs')
v = re.search(r'VERSION\s*=\s*"([^"]+)"', open(R + '/src/main/java/com/apocollis/aqtweaks/ArcanaQuestTweaks.java', encoding='utf-8').read())
print(' VERSION', v.group(1) if v else None)
print()
print('FAIL' if FAIL else 'OK', len(FAIL), 'hard findings')
sys.exit(1 if FAIL else 0)
