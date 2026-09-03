import paramiko

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    print("Testing 176.111.109.14 with old password...")
    client.connect('176.111.109.14', username='root', password='IPTV_Private_VPS_2026_Secure!', timeout=5)
    print("Success!")
except paramiko.AuthenticationException:
    print("Authentication Failed!")
except Exception as e:
    print(f"Error: {e}")
