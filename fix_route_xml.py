# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_mobile_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

import re
xml = re.sub(r'app:mediaRouteTypes="[^"]*"', '', xml)

with open('app/src/main/res/layout/activity_mobile_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

print("Done")
