package com.iptv.app

data class Category(
    val category_id: String,
    val category_name: String,
    val parent_id: Int
)

data class Stream(
    val stream_id: String,
    val name: String,
    val stream_icon: String,
    val stream_type: String,
    val extension: String = "mp4",
    val added: String = "0"
)

data class Episode(
    val id: String,
    val episode_num: Int,
    val title: String,
    val container_extension: String,
    val seasonNum: String = ""
)
