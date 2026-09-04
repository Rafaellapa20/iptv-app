import paramiko
try:
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect("176.111.109.14", username="root", password="Eqa796*5F(spSO", timeout=5)
    
    stdin, stdout, stderr = client.exec_command("ls -la /var/www/html/iptv_app")
    print(stdout.read().decode('utf-8', errors='ignore'))
    
    stdin, stdout, stderr = client.exec_command("systemctl status nginx --no-pager")
    print(stdout.read().decode('utf-8', errors='ignore'))
    client.close()
except Exception as e:
    print(e)
