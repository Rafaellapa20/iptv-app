# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_player.xml', 'r', encoding='utf-8') as f:
    text = f.read()

import re
# We need to remove the second occurrence of tvDiagnostics block
# The block is roughly:
#        <TextView
#            android:id="@+id/tvDiagnostics"
# ...
#            android:textStyle="italic" />

block_regex = re.compile(r'\s*<TextView\s+android:id="@+id/tvDiagnostics"[^>]+/>')
matches = block_regex.findall(text)
if len(matches) > 1:
    text = text.replace(matches[1], "", 1)
    
with open('app/src/main/res/layout/activity_player.xml', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
