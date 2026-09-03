# -*- coding: utf-8 -*-
files = ['app/src/main/res/layout/item_mini_guia.xml']
for f_path in files:
    with open(f_path, 'rb') as f:
        content = f.read()
    if content.startswith(b'\xef\xbb\xbf'):
        content = content[3:]
    with open(f_path, 'wb') as f:
        f.write(content)
print("BOM removed")
