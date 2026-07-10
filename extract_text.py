import html, re, sys

with open('F:/Downloads/БОЛЬШОЙ АПГРЕЙД_ копия игрока вместо зомби + чат-ИИ + разрушение мира по всему измерению-20260709230436.html', 'r', encoding='utf-8', errors='replace') as f:
    content = f.read()

content = re.sub(r'<style[^>]*>.*?</style>', '', content, flags=re.DOTALL)
content = re.sub(r'<script[^>]*>.*?</script>', '', content, flags=re.DOTALL)
text = re.sub(r'<[^>]+>', '\n', content)
text = html.unescape(text)
text = text.replace('&amp;', '&').replace('&lt;', '<').replace('&gt;', '>')
text = text.replace('&quot;', '"').replace('&#x2F;', '/').replace('&#x27;', "'")
text = re.sub(r' {2,}', ' ', text)
text = re.sub(r'\n{3,}', '\n\n', text)
lines = [l.strip() for l in text.split('\n') if l.strip()]
sys.stdout.buffer.write('\n'.join(lines).encode('utf-8'))
