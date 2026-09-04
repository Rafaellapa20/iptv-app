# -*- coding: utf-8 -*-

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re

# Replace the try block inside fetchMoviesPosters
movies_old = """                    val list = mutableListOf<Pair<Stream, Long>>()
                    
                    try {
                        // Sometimes the API might return an object with error or no data. Check token.
                        if (reader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                reader.beginObject()
                                var streamId = ""
                                var name = ""
                                var icon = ""
                                var added = 0L
                                while (reader.hasNext()) {
                                    val key = reader.nextName()
                                    if (reader.peek() == android.util.JsonToken.NULL) {
                                        reader.skipValue()
                                        continue
                                    }
                                    when (key) {
                                        "stream_id" -> streamId = reader.nextString()
                                        "name" -> name = reader.nextString()
                                        "stream_icon" -> icon = reader.nextString()
                                        "added" -> added = reader.nextString().toLongOrNull() ?: 0L
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                if (icon.isNotEmpty()) {
                                    list.add(Stream(streamId, name, icon, "movie", "mp4") to added)
                                }
                            }
                            reader.endArray()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try { reader.close() } catch(e:Exception){}
                    }

                    list.sortByDescending { it.second }
                    recentMovies.addAll(list.map { it.first }.take(30))"""

movies_new = """                    val pq = java.util.PriorityQueue<Pair<Stream, Long>>(31) { a, b -> a.second.compareTo(b.second) }
                    try {
                        if (reader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                if (reader.peek() != android.util.JsonToken.BEGIN_OBJECT) {
                                    reader.skipValue()
                                    continue
                                }
                                reader.beginObject()
                                var streamId = ""
                                var name = ""
                                var icon = ""
                                var added = 0L
                                while (reader.hasNext()) {
                                    val key = reader.nextName()
                                    val token = reader.peek()
                                    if (token == android.util.JsonToken.NULL) {
                                        reader.skipValue()
                                        continue
                                    }
                                    when (key) {
                                        "stream_id" -> {
                                            if (token == android.util.JsonToken.NUMBER || token == android.util.JsonToken.STRING) streamId = reader.nextString() else reader.skipValue()
                                        }
                                        "name" -> {
                                            if (token == android.util.JsonToken.NUMBER || token == android.util.JsonToken.STRING) name = reader.nextString() else reader.skipValue()
                                        }
                                        "stream_icon" -> {
                                            if (token == android.util.JsonToken.STRING) icon = reader.nextString() else reader.skipValue()
                                        }
                                        "added" -> {
                                            if (token == android.util.JsonToken.NUMBER || token == android.util.JsonToken.STRING) added = reader.nextString().toLongOrNull() ?: 0L else reader.skipValue()
                                        }
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                if (icon.isNotEmpty()) {
                                    pq.offer(Stream(streamId, name, icon, "movie", "mp4") to added)
                                    if (pq.size > 30) pq.poll()
                                }
                            }
                            reader.endArray()
                        } else if (reader.peek() == android.util.JsonToken.BEGIN_OBJECT) {
                            // If it's an object containing categories as keys
                            reader.beginObject()
                            while(reader.hasNext()) {
                                reader.nextName() // skip key
                                if (reader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                                    reader.beginArray()
                                    while (reader.hasNext()) {
                                        if (reader.peek() == android.util.JsonToken.BEGIN_OBJECT) {
                                            reader.beginObject()
                                            var streamId = ""
                                            var name = ""
                                            var icon = ""
                                            var added = 0L
                                            while (reader.hasNext()) {
                                                val key = reader.nextName()
                                                val token = reader.peek()
                                                if (token == android.util.JsonToken.NULL) { reader.skipValue(); continue }
                                                when (key) {
                                                    "stream_id" -> if (token == android.util.JsonToken.NUMBER || token == android.util.JsonToken.STRING) streamId = reader.nextString() else reader.skipValue()
                                                    "name" -> if (token == android.util.JsonToken.NUMBER || token == android.util.JsonToken.STRING) name = reader.nextString() else reader.skipValue()
                                                    "stream_icon" -> if (token == android.util.JsonToken.STRING) icon = reader.nextString() else reader.skipValue()
                                                    "added" -> if (token == android.util.JsonToken.NUMBER || token == android.util.JsonToken.STRING) added = reader.nextString().toLongOrNull() ?: 0L else reader.skipValue()
                                                    else -> reader.skipValue()
                                                }
                                            }
                                            reader.endObject()
                                            if (icon.isNotEmpty()) {
                                                pq.offer(Stream(streamId, name, icon, "movie", "mp4") to added)
                                                if (pq.size > 30) pq.poll()
                                            }
                                        } else {
                                            reader.skipValue()
                                        }
                                    }
                                    reader.endArray()
                                } else {
                                    reader.skipValue()
                                }
                            }
                            reader.endObject()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try { reader.close() } catch(e:Exception){}
                    }

                    val sorted = pq.toList().sortedByDescending { it.second }
                    recentMovies.addAll(sorted.map { it.first })"""

text = text.replace(movies_old, movies_new)

# Series 
series_old = """                    try {
                        if (reader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                reader.beginObject()
                                var seriesId = ""
                                var name = ""
                                var icon = ""
                                while (reader.hasNext()) {
                                    val key = reader.nextName()
                                    if (reader.peek() == android.util.JsonToken.NULL) {
                                        reader.skipValue()
                                        continue
                                    }
                                    when (key) {
                                        "series_id" -> seriesId = reader.nextString()
                                        "name" -> name = reader.nextString()
                                        "cover" -> icon = reader.nextString()
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                if (icon.isNotEmpty()) {
                                    recentSeries.add(Stream(seriesId, name, icon, "series", ""))
                                }
                            }
                            reader.endArray()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try { reader.close() } catch(e:Exception){}
                    }

                    recentSeries.shuffle()
                    val toShow = recentSeries.take(30)
                    recentSeries.clear()
                    recentSeries.addAll(toShow)"""

series_new = """                    try {
                        if (reader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                if (reader.peek() != android.util.JsonToken.BEGIN_OBJECT) {
                                    reader.skipValue()
                                    continue
                                }
                                reader.beginObject()
                                var seriesId = ""
                                var name = ""
                                var icon = ""
                                while (reader.hasNext()) {
                                    val key = reader.nextName()
                                    val token = reader.peek()
                                    if (token == android.util.JsonToken.NULL) {
                                        reader.skipValue()
                                        continue
                                    }
                                    when (key) {
                                        "series_id" -> if (token == android.util.JsonToken.NUMBER || token == android.util.JsonToken.STRING) seriesId = reader.nextString() else reader.skipValue()
                                        "name" -> if (token == android.util.JsonToken.NUMBER || token == android.util.JsonToken.STRING) name = reader.nextString() else reader.skipValue()
                                        "cover" -> if (token == android.util.JsonToken.STRING) icon = reader.nextString() else reader.skipValue()
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                if (icon.isNotEmpty()) {
                                    if (recentSeries.size < 50) {
                                        recentSeries.add(Stream(seriesId, name, icon, "series", ""))
                                    }
                                }
                            }
                            reader.endArray()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try { reader.close() } catch(e:Exception){}
                    }
                    recentSeries.shuffle()
"""

text = text.replace(series_old, series_new)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

print("Fixed JSON parsers")
