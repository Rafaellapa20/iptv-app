# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/LiveTvActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('val namesList = ArrayList<String>()', 'val namesList = ArrayList<String>()\n                        val coversList = ArrayList<String>()')
text = text.replace('namesList.add(item.name)', 'namesList.add(item.name)\n                            coversList.add(item.stream_icon)')

with open('app/src/main/java/com/iptv/app/LiveTvActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

with open('app/src/main/res/layout/item_mini_guia.xml', 'r', encoding='utf-8') as f:
    item_xml = f.read()
item_xml = item_xml.replace('@drawable/bg_button', '#333333')
with open('app/src/main/res/layout/item_mini_guia.xml', 'w', encoding='utf-8') as f:
    f.write(item_xml)

with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'r', encoding='utf-8') as f:
    p_text = f.read()
p_text = p_text.replace('holder.itemView.setBackgroundResource(R.drawable.bg_button)', 'holder.itemView.setBackgroundColor(android.graphics.Color.parseColor("#333333"))')
with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'w', encoding='utf-8') as f:
    f.write(p_text)

print("Done")
