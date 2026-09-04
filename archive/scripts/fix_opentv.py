import re

with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'r', encoding='utf-8') as f:
    code = f.read()

# Replace openTv logic
old_logic = '''        val openTv = View.OnClickListener {
            // "nos canais quero que abra o canal antes visto"
            val recents = RecentManager.getRecent(this).filter { it.stream_type == "live" }
            if (recents.isNotEmpty()) {
                playChannel(recents[0], recents, username, password)
                return@OnClickListener
            }
            val intent = Intent(this@SeniorMainActivity, LiveTvActivity::class.java)
            intent.putExtra("TYPE", "live")
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }'''

new_logic = '''        val openTv = View.OnClickListener {
            val intent = Intent(this@SeniorMainActivity, LiveTvActivity::class.java)
            intent.putExtra("TYPE", "live")
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }'''

code = code.replace(old_logic, new_logic)

with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(code)

print("Fixed openTv logic")
