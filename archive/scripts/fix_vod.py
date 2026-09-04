# -*- coding: utf-8 -*-
import re

with open('app/src/main/java/com/iptv/app/VodNetflixActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('▶️ Continuar a Assistir', '▶️ Continuar a Ver')
text = text.replace('❤️ A Minha Lista', '❤️ Favoritos')
text = text.replace('my_list', 'favorites')

text = re.sub(
    r'private val genreMap = linkedMapOf\(\s*\"▶️ Continuar a Ver\" to listOf\(\"continue_watching\"\),\s*\"❤️ Favoritos\" to listOf\(\"favorites\"\),',
    'private val genreMap = linkedMapOf(\n        "▶️ Continuar a Ver" to listOf("continue_watching"),\n        "✅ Já Visto" to listOf("already_watched"),\n        "❤️ Favoritos" to listOf("favorites"),',
    text
)

search_logic = '''// --- CONTINUAR A VER ---
                    val recent = ProgressManager.getRecentProgressList(this@VodNetflixActivity).filter { it.type == type }
                    if (recent.isNotEmpty()) {
                        val sList = recent.map { Stream(it.streamId, it.title, it.coverUrl, it.type, "") }
                        categories.add(Category("continue_watching", "▶️ Continuar a Ver", 0))
                        streamsByCategory["continue_watching"] = sList
                    }'''
                    
replace_logic = '''// --- CONTINUAR A VER e JÁ VISTO ---
                    val recent = ProgressManager.getRecentProgressList(this@VodNetflixActivity).filter { it.type == type }
                    
                    val continueWatchingList = recent.filter { 
                        it.duration == 0L || (it.position.toDouble() / it.duration) < 0.90
                    }
                    if (continueWatchingList.isNotEmpty()) {
                        val sList = continueWatchingList.map { Stream(it.streamId, it.title, it.coverUrl, it.type, "") }
                        categories.add(Category("continue_watching", "▶️ Continuar a Ver", 0))
                        streamsByCategory["continue_watching"] = sList
                    }

                    val alreadyWatchedList = recent.filter { 
                        it.duration > 0L && (it.position.toDouble() / it.duration) >= 0.90
                    }
                    if (alreadyWatchedList.isNotEmpty()) {
                        val sList = alreadyWatchedList.map { Stream(it.streamId, it.title, it.coverUrl, it.type, "") }
                        categories.add(Category("already_watched", "✅ Já Visto", 0))
                        streamsByCategory["already_watched"] = sList
                    }'''

# The source file might already have it changed due to earlier regexes. Let's just do a reliable replace:
if '// --- CONTINUAR A VER e JÁ VISTO ---' not in text:
    text = text.replace(search_logic, replace_logic)

text = text.replace('if (genreName == "▶️ Continuar a Ver" || genreName == "❤️ Favoritos") continue', 'if (genreName == "▶️ Continuar a Ver" || genreName == "✅ Já Visto" || genreName == "❤️ Favoritos") continue')

with open('app/src/main/java/com/iptv/app/VodNetflixActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
