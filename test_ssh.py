import paramiko

def test_ssh(ip, user, pwd):
    try:
        client = paramiko.SSHClient()
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        client.connect(ip, username=user, password=pwd, timeout=5)
        print(f"Success connecting to {ip}")
        
        stdin, stdout, stderr = client.exec_command("ls -la /var/www/html")
        print(stdout.read().decode('utf-8'))
        client.close()
    except Exception as e:
        print(f"Failed connecting to {ip}: {e}")

test_ssh("176.111.109.14", "root", "IPTV_Private_VPS_2026_Secure!")
test_ssh("65.21.178.77", "root", "IPTV_Private_VPS_2026_Secure!")
