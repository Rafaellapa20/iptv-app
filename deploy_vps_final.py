import paramiko
import os

ip = "176.111.109.14"
pwd = "Eqa796*5F(spSO"

try:
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(ip, username="root", password=pwd, timeout=10)
    print("SSH Connected!")
    
    # 1. Install Nginx & Create Dir
    stdin, stdout, stderr = client.exec_command("apt-get update && apt-get install -y nginx && mkdir -p /var/www/html/iptv_app")
    print("Setup Nginx:", stdout.read().decode('utf-8'))
    
    sftp = client.open_sftp()
    
    # Upload current ones as placeholders
    sftp.put("iptv_mobile.apk", "/var/www/html/iptv_app/iptv_mobile.apk")
    sftp.put("update.json", "/var/www/html/iptv_app/update.json")
    
    sftp.close()
    
    # Set permissions
    client.exec_command("chmod -R 755 /var/www/html/iptv_app")
    print("Files uploaded successfully.")
    
    client.close()
except Exception as e:
    print(f"Error: {e}")
