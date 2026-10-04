#!/usr/bin/env python3
"""Fail if a mixin handler names a method that its target class does not declare.

Mixin only searches the target class itself, not its superclasses, and with no
`injectors.defaultRequire` a missing method is skipped silently. This script is the
dev-time substitute.

Vanilla targets (net.minecraft.*) are checked against the MCP/SRG snapshot
(srg_to_snapshot_*.tsrg, found in the gradle cache or via --tsrg). Parent-mod targets are
checked with `javap -p` against jars in libs/. Targets that cannot be resolved are listed as
UNVERIFIED and do not fail the run.

Usage:  python scripts/check_mixin_targets.py [--tsrg PATH] [--strict]
Methods added to a target at runtime by another mod's mixin go in ALLOW below.
"""
import argparse
import collections
import glob
import os
import re
import subprocess
import sys
import zipfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
MIXIN_DIR = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'apocollis', 'aqtweaks', 'mixin')

# (target class, method) pairs that exist only at runtime (added by another mod's mixin).
ALLOW = {
    ('thaumcraft.common.items.baubles.ItemBaubles', 'getModel'),  # BaublesEX mixin
}


def find_tsrg(explicit):
    if explicit:
        return explicit
    home = os.path.expanduser('~')
    hits = glob.glob(os.path.join(home, '.gradle', 'caches', 'forge_gradle', 'minecraft_user_repo',
                                  'de', 'oceanlabs', 'mcp', 'mcp_config', '*', 'srg_to_snapshot_*.tsrg'))
    return hits[0] if hits else None


def load_vanilla(tsrg):
    decl = collections.defaultdict(set)
    cur = None
    with open(tsrg, encoding='utf8') as fh:
        for line in fh:
            line = line.rstrip('\n')
            if not line.startswith('\t'):
                cur = line.split(' ')[0]
                continue
            parts = line.strip().split(' ')
            if len(parts) == 2:
                decl[cur].update(parts)
            elif len(parts) == 3:
                decl[cur].update((parts[0], parts[2]))
    return decl


def jar_index():
    idx = {}
    for jar in glob.glob(os.path.join(ROOT, 'libs', '*.jar')):
        try:
            with zipfile.ZipFile(jar) as z:
                for n in z.namelist():
                    if n.endswith('.class'):
                        idx.setdefault(n[:-6], jar)
        except zipfile.BadZipFile:
            pass
    return idx


_members = {}


def parent_members(idx, cls):
    if cls in _members:
        return _members[cls]
    jar = idx.get(cls.replace('.', '/'))
    names = None
    if jar:
        out = subprocess.run(['javap', '-p', '-cp', jar, cls], capture_output=True, text=True).stdout
        names = set()
        for line in out.splitlines():
            m = re.search(r'([\w$]+)\(', line)
            if m:
                names.add(m.group(1))
    _members[cls] = names
    return names


def mixin_target(text):
    imports = {m.group(2): m.group(1) + '.' + m.group(2)
               for m in re.finditer(r'import ([\w.]+)\.(\w+);', text)}
    mm = re.search(r'@Mixin\((.*?)\)\s*(?:public\s+|abstract\s+|final\s+)*(?:class|interface)', text, re.S)
    if not mm:
        return None
    arg = mm.group(1)
    m2 = re.search(r'targets\s*=\s*"([^"]+)"', arg)
    if m2:
        return m2.group(1)
    m1 = re.search(r'value\s*=\s*([\w.]+)\.class', arg) or re.match(r'\s*([\w.]+)\.class', arg)
    if m1:
        name = m1.group(1)
        parts = name.split('.')
        # Nested class written as Outer.Inner: resolve Outer through the imports, join with '$'.
        if parts[0] in imports:
            return imports[parts[0]] + ''.join('$' + p for p in parts[1:])
        return name
    return None


def method_names(text):
    for mt in re.finditer(r'method\s*=\s*(\{[^}]*\}|"[^"]*")', text):
        for n in re.findall(r'"([^"]*)"', mt.group(1)):
            name = re.sub(r'\(.*', '', n).split(';')[-1]
            if name not in ('<init>', '<clinit>'):
                yield n, name


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--tsrg')
    ap.add_argument('--strict', action='store_true', help='fail on UNVERIFIED targets too')
    args = ap.parse_args()

    tsrg = find_tsrg(args.tsrg)
    vanilla = load_vanilla(tsrg) if tsrg else {}
    if not tsrg:
        print('WARN: no srg_to_snapshot tsrg found; vanilla targets will be UNVERIFIED')
    idx = jar_index()

    bad, unverified, checked = [], [], 0
    for dp, _dn, fns in os.walk(MIXIN_DIR):
        for fn in sorted(fns):
            if not fn.endswith('.java'):
                continue
            text = open(os.path.join(dp, fn), encoding='utf8').read()
            cls = mixin_target(text)
            if not cls:
                continue
            methods = list(method_names(text))
            if not methods:
                continue
            if cls.startswith('net.minecraft.'):
                key = cls.replace('.', '/')
                members = vanilla.get(key)
            else:
                members = parent_members(idx, cls)
            if members is None:
                unverified.append((fn, cls))
                continue
            for raw, name in methods:
                checked += 1
                if (cls, name) in ALLOW:
                    continue
                if name not in members:
                    bad.append((fn, cls, raw))

    for fn, cls in sorted(set(unverified)):
        print('UNVERIFIED  %-45s %s' % (fn, cls))
    for fn, cls, raw in bad:
        print('NOT DECLARED %-44s %s  method=%s' % (fn, cls, raw))
    print('checked %d method refs, %d not declared, %d targets unverified' % (checked, len(bad), len(set(unverified))))
    return 1 if bad or (args.strict and unverified) else 0


if __name__ == '__main__':
    sys.exit(main())
