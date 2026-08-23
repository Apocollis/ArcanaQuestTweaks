import re
import collections
import sys

p = sys.argv[1]
with open(p, "rb") as f:
    data = f.read()

methods = collections.Counter()
idx = 0
while True:
    i = data.find(b"aqtweaks", idx)
    if i < 0:
        break
    t = "".join(chr(c) if 32 <= c < 127 else "." for c in data[max(0, i - 140) : i + 180])
    for m in re.finditer(r"com\.apocollis\.aqtweaks\.([A-Za-z0-9_.]+)\"\.([A-Za-z0-9_$]+)", t):
        methods[m.group(1) + "." + m.group(2)] += 1
    for m in re.finditer(r"aqtweaks\$[A-Za-z0-9_]+", t):
        methods["mixin." + m.group(0)] += 1
    idx = i + 8

print("--- aqtweaks methods ---")
for k, v in methods.most_common(90):
    print("%6d %s" % (v, k))
