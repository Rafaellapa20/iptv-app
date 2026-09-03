# -*- coding: utf-8 -*-
import os

files = [
    'app/src/main/res/layout/activity_senior_main.xml'
]

for f in files:
    try:
        with open(f, 'rb') as file:
            content = file.read()
        if content.startswith(b'\xef\xbb\xbf'):
            content = content[3:]
            with open(f, 'wb') as file:
                file.write(content)
            print(f"Removed BOM from {f}")
    except Exception as e:
        print(f"Error {f}: {e}")

print("Done")
