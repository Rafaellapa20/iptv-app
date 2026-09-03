import paramiko
import sys

sys.stdout.reconfigure(encoding='utf-8')

ip = "176.111.109.14"
pwd = "Eqa796*5F(spSO"

try:
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(ip, username="root", password=pwd, timeout=10)
    
    sftp = client.open_sftp()
    
    print("Uploading APK to VPS...")
    sftp.put("iptv_mobile.apk", "/var/www/html/iptv_app/iptv_mobile.apk")
    print("Uploading JSON to VPS...")
    sftp.put("update.json", "/var/www/html/iptv_app/update.json")
    
    sftp.close()
    
    client.exec_command("chmod -R 755 /var/www/html/iptv_app")
    print("All files successfully uploaded and live on the VPS!")
    
    client.close()
except Exception as e:
    print(f"Error: {e}")
