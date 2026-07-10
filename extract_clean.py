import html, re, sys

with open('F:/Downloads/Liminal — актуальный полный код (v2, все фиксы внедрены)-20260709220442.html', 'r', encoding='utf-8', errors='replace') as f:
    content = f.read()
pre_blocks = re.findall(r'<pre[^>]*>(.*?)</pre>', content, re.DOTALL)
for i, block in enumerate(pre_blocks):
    text = html.unescape(block)
    text = re.sub(r'<[^>]+>', '', text)
    text = html.unescape(text)
    text = text.replace('&amp;', '&').replace('&lt;', '<').replace('&gt;', '>')
    text = text.replace('&quot;', '"').replace('&#x2F;', '/').replace('&#x27;', "'")
    clean = ''.join(c if ord(c) < 128 or c in '{}[]();:\\"' else ' ' for c in text)
    clean = re.sub(r' {2,}', ' ', clean)
    sys.stdout.buffer.write(('=== BLOCK ' + str(i+1) + ' ===\n').encode('utf-8'))
    sys.stdout.buffer.write((clean + '\n\n').encode('utf-8'))
