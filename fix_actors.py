# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/MovieInfoActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re

search = r'holder\.photo\.setImageResource\(android\.R\.drawable\.ic_menu_myplaces\)'
replace = '''val encodedName = android.net.Uri.encode(actor.name)
            val avatarUrl = "https://ui-avatars.com/api/?name=\&background=222222&color=ffffff&size=200&bold=true"
            Glide.with(holder.itemView.context).load(avatarUrl).circleCrop().into(holder.photo)'''
text = text.replace(search, replace)

with open('app/src/main/java/com/iptv/app/MovieInfoActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

print("Done")
