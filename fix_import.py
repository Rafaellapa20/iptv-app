# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('import kotlinx.coroutines.withContext', 'import kotlinx.coroutines.withContext\nimport kotlinx.coroutines.delay')

with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
