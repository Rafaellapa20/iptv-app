import codecs

# Read raw bytes
with open('app/src/main/res/layout/activity_main.xml', 'rb') as f:
    raw = f.read()

# Decode as UTF-8
try:
    text = raw.decode('utf-8')
    # If it has double encoding, we can try to fix it
    if '?' in text:
        text = raw.decode('utf-8').encode('iso-8859-1').decode('utf-8')
except Exception as e:
    print('Decode error', e)
    text = raw.decode('utf-8', errors='ignore')

# Fix buttons correctly
text = text.replace('??? SpeedTest', '?? Defini??es')
text = text.replace('???? Emparelhar', '?? Emparelhar')
text = text.replace('??? Defini????es', '?? Modo F?cil')
text = text.replace('v10.50', 'v10.54')

# Replace the background of btnQuickCatchup safely
text = text.replace('android:id="@+id/btnQuickCatchup"\n              android:layout_width="0dp"\n              android:layout_height="match_parent"\n              android:layout_weight="1"\n              android:background="@drawable/bg_smarters_sage"', 'android:id="@+id/btnQuickCatchup"\n              android:layout_width="0dp"\n              android:layout_height="match_parent"\n              android:layout_weight="1"\n              android:background="#2E7D32"')

with open('app/src/main/res/layout/activity_main.xml', 'wb') as f:
    f.write(text.encode('utf-8'))

print("Fixed XML")
