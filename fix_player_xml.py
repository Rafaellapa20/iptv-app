# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_player.xml', 'r', encoding='utf-8') as f:
    text = f.read()

import re

# We have an extra piece:
#     </androidx.cardview.widget.CardView>
#         </LinearLayout>
# 
#         <ProgressBar
#             android:id="@+id/pbEpgProgress"
# ...
#             android:textSize="12sp" />
#     </LinearLayout>

search = r'</androidx\.cardview\.widget\.CardView>\s*</LinearLayout>\s*<ProgressBar\s*android:id="@+id/pbEpgProgress".*?android:textSize="12sp" />\s*</LinearLayout>'
replace = '</androidx.cardview.widget.CardView>'

text = re.sub(search, replace, text, flags=re.DOTALL)

with open('app/src/main/res/layout/activity_player.xml', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
