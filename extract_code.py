import html, re, sys

def extract_all_code(filepath):
    with open(filepath, 'r', encoding='utf-8', errors='replace') as f:
        content = f.read()
    pre_blocks = re.findall(r'<pre[^>]*>(.*?)</pre>', content, re.DOTALL)
    for block in pre_blocks:
        text = html.unescape(block)
        text = re.sub(r'<[^>]+>', '', text)
        text = html.unescape(text)
        text = text.replace('&amp;', '&').replace('&lt;', '<').replace('&gt;', '>')
        text = text.replace('&quot;', '"').replace('&#x2F;', '/').replace('&#x27;', "'")
        sys.stdout.buffer.write((text + '\n---FILEBREAK---\n').encode('utf-8'))

if __name__ == '__main__':
    extract_all_code(sys.argv[1])
