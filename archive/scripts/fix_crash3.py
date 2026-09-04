import re

with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

kt = kt.replace('RecentManager.RecentStream', 'Stream')

with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Crash fix 3 applied.")
