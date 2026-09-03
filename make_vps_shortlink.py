import urllib.request
import urllib.parse

long_url = "http://176.111.109.14/iptv_app/iptv_mobile.apk"
api_url = "http://tinyurl.com/api-create.php?url=" + urllib.parse.quote(long_url)

try:
    response = urllib.request.urlopen(api_url)
    short_url = response.read().decode('utf-8')
    print("SHORT_URL=" + short_url)
except Exception as e:
    print("Error:", e)
