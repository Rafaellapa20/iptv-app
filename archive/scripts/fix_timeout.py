# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/OkHttpProvider.kt', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('readTimeout(12, TimeUnit.SECONDS)', 'readTimeout(30, TimeUnit.SECONDS)')
text = text.replace('connectTimeout(12, TimeUnit.SECONDS)', 'connectTimeout(20, TimeUnit.SECONDS)')

with open('app/src/main/java/com/iptv/app/OkHttpProvider.kt', 'w', encoding='utf-8') as f:
    f.write(text)

print("Fixed OkHttp timeout")
