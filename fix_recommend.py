# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/VodNetflixActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re

search = '''                    // Grupos de categorias
                    val genreToCatIds = mutableMapOf<String, MutableList<String>>()'''

replace = '''
                    // --- RECOMENDADOS PARA SI ---
                    // Pegar em 15 filmes aleatórios não assistidos
                    if (fetchedStreams.isNotEmpty()) {
                        val watchedIds = recent.map { it.streamId }.toSet()
                        val notWatched = fetchedStreams.filter { !watchedIds.contains(it.stream_id) }
                        if (notWatched.isNotEmpty()) {
                            val shuffled = notWatched.shuffled(java.util.Random(System.currentTimeMillis())).take(15)
                            categories.add(Category("recommended", "💡 Recomendados para Si", 0))
                            streamsByCategory["recommended"] = shuffled
                        }
                    }

                    // Grupos de categorias
                    val genreToCatIds = mutableMapOf<String, MutableList<String>>()'''

if 'Recomendados para Si' not in text:
    text = text.replace(search, replace)

with open('app/src/main/java/com/iptv/app/VodNetflixActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
