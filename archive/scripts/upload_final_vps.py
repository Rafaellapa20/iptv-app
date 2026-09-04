import os
import subprocess
import json

print('Uploading to VPS...')
subprocess.run(['scp', 'app/build/outputs/apk/debug/app-debug.apk', 'root@176.111.109.14:/var/www/html/iptv_app/iptv_mobile.apk'])
subprocess.run(['scp', 'update.json', 'root@176.111.109.14:/var/www/html/iptv_app/update.json'])
print('Upload complete!')
