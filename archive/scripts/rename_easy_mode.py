# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    main_xml = f.read()

# Replace button text
main_xml = main_xml.replace('👴 Modo Idosos', '🟢 Modo Fácil')

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(main_xml)

with open('app/src/main/res/layout/activity_senior_main.xml', 'r', encoding='utf-8') as f:
    senior_xml = f.read()

# Replace header text
senior_xml = senior_xml.replace('MODO SIMPLIFICADO', 'MODO FÁCIL')

with open('app/src/main/res/layout/activity_senior_main.xml', 'w', encoding='utf-8') as f:
    f.write(senior_xml)

print("Done")
