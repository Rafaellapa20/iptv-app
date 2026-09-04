# -*- coding: utf-8 -*-
import re

with open('app/src/main/java/com/iptv/app/VodNetflixActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Remove transition animation which often crashes older Android TV boxes
search = r'''ViewCompat\.setTransitionName\(ivPoster, "poster_transition"\)\s*val options = ActivityOptionsCompat\.makeSceneTransitionAnimation\(\s*this@VodNetflixActivity, ivPoster, "poster_transition"\s*\)\s*startActivity\(intent, options\.toBundle\(\)\)'''
replace = '''startActivity(intent)'''
text = re.sub(search, replace, text)

# Change SeriesInfoActivity to MovieInfoActivity for series (temporarily fallback so it doesn't crash, we'll treat a series like a movie for the UI temporarily, or wait, Series don't have VIDEO_URL directly!)
# No, let's leave SeriesInfoActivity and create it.

with open('app/src/main/java/com/iptv/app/VodNetflixActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

print("Done")
