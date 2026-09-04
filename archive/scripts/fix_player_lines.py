# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_player.xml', 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = lines[:210] + lines[229:]

with open('app/src/main/res/layout/activity_player.xml', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)
print("Done")
