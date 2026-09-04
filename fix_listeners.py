with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

kt = kt.replace('findViewById<View>(R.id.btnSeniorFilmes).setOnClickListener(openMovies)',
'''findViewById<View>(R.id.btnSeniorFilmes).setOnClickListener(openMovies)
        findViewById<View>(R.id.btnSeniorSeries).setOnClickListener(openSeries)
        findViewById<View>(R.id.btnSeniorFavoritos).setOnClickListener(openFavorites)''')

with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)
print("Fixed listeners.")
