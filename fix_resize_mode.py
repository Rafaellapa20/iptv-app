# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_live_tv.xml', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('app:resize_mode="fill"', 'app:resize_mode="fit"')

with open('app/src/main/res/layout/activity_live_tv.xml', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
