# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('private lateinit var tvLoadingTitle: android.widget.TextView', 'private lateinit var tvLoadingTitle: android.widget.TextView\n    private lateinit var tvDiagnostics: android.widget.TextView')

with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
