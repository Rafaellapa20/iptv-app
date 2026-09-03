import urllib.request
import urllib.parse

long_url = "https://raw.githubusercontent.com/Rafaellapa20/iptv-app/main/iptv_mobile.apk"
api_url = "http://tinyurl.com/api-create.php?url=" + urllib.parse.quote(long_url)

try:
    response = urllib.request.urlopen(api_url)
    short_url = response.read().decode('utf-8')
    print("SHORT_URL=" + short_url)
except Exception as e:
    print("Error:", e)
