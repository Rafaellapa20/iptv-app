# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

# Add onBackPressed override
exit_logic = '''
    override fun onBackPressed() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Tem a certeza que deseja sair da aplicação?")
            .setPositiveButton("Sim") { _, _ ->
                finishAffinity() // Closes all activities
            }
            .setNegativeButton("Não", null)
            .show()
    }
'''

if 'override fun onBackPressed()' not in kt:
    kt = kt.replace('override fun onDestroy()', exit_logic + '\n    override fun onDestroy()')

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("MainActivity updated")
