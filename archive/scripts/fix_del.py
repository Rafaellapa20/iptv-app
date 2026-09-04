# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_player.xml', 'r', encoding='utf-8') as f:
    lines = f.readlines()

with open('app/src/main/res/layout/activity_player.xml', 'w', encoding='utf-8') as f:
    for i, line in enumerate(lines):
        if i >= 228:
            break
        f.write(line)
        
with open('app/src/main/res/layout/activity_player.xml', 'a', encoding='utf-8') as f:
    f.write('    </RelativeLayout>\n</FrameLayout>\n')

print("Done")
