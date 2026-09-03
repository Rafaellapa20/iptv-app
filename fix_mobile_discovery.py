# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/MobileMainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re
search = r'findViewById<Button>\(R\.id\.btnMobileRemote\)\.setOnClickListener \{[\s\S]*?\}'
replace = '''val btnRemote = findViewById<Button>(R.id.btnMobileRemote)
        btnRemote.setOnClickListener {
            btnRemote.text = "A procurar..."
            RemoteManager.discoverTv(this, 
                onFound = { ip -> 
                    Toast.makeText(this, "TV Ligada! IP: \", Toast.LENGTH_SHORT).show()
                    btnRemote.text = "Ligado à TV"
                    btnRemote.setBackgroundColor(android.graphics.Color.parseColor("#00E5FF"))
                },
                onTimeout = {
                    Toast.makeText(this, "Não foi possível encontrar a TV na rede.", Toast.LENGTH_SHORT).show()
                    btnRemote.text = "Ligar à TV (Tentar Novamente)"
                }
            )
        }'''

text = re.sub(search, replace, text, count=1)

with open('app/src/main/java/com/iptv/app/MobileMainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

print("Done")
