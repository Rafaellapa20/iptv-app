import re
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    text = f.read()
for match in re.findall(r'android:text="(.*?)"', text):
    print(match.encode('unicode_escape').decode('utf-8'))
