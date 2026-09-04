import paramiko

try:
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect("65.21.178.77", username="root", password="IPTV_Private_VPS_2026_Secure!", timeout=10)
    print("Connected to VPS")
    
    # 1. Install Nginx if not installed
    stdin, stdout, stderr = client.exec_command("apt-get update && apt-get install -y nginx")
    stdout.read()
    
    # 2. Create directory
    client.exec_command("mkdir -p /var/www/html/app")
    
    # 3. Upload files
    sftp = client.open_sftp()
    print("Uploading APK...")
    sftp.put("iptv_mobile.apk", "/var/www/html/app/iptv_mobile.apk")
    print("Uploading update.json...")
    sftp.put("update.json", "/var/www/html/app/update.json")
    
    # 4. Set permissions
    client.exec_command("chmod -R 755 /var/www/html/app")
    
    print("Success! App is now hosted on the VPS.")
    sftp.close()
    client.close()
except Exception as e:
    print(f"Deployment error: {e}")
