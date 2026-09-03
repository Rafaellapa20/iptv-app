# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

exit_logic = '''
    override fun onBackPressed() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Tem a certeza que deseja sair da aplicação?")
            .setPositiveButton("Sim") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("Não", null)
            .show()
    }
'''

if 'override fun onBackPressed()' not in kt:
    kt = kt.replace('override fun onDestroy()', exit_logic + '\n    override fun onDestroy()')

with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("SeniorMainActivity updated")
