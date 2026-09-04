import re

with open('app/src/main/java/com/iptv/app/UpdateManager.kt', 'r', encoding='utf-8') as f:
    code = f.read()

code = re.sub(
    r'private const val UPDATE_JSON_URL = "http://176\.111\.109\.14/iptv_app/update\.json"',
    'private const val UPDATE_JSON_URL = "https://raw.githubusercontent.com/Rafaellapa20/iptv-app/main/update.json"',
    code
)

with open('app/src/main/java/com/iptv/app/UpdateManager.kt', 'w', encoding='utf-8') as f:
    f.write(code)

print("Updated UpdateManager.kt")
