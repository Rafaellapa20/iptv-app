# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re

vars_search = 'private lateinit var tvChannelNumber: android.widget.TextView'
vars_replace = '''private lateinit var tvChannelNumber: android.widget.TextView
    private lateinit var llMiniGuia: View
    private lateinit var rvMiniGuia: androidx.recyclerview.widget.RecyclerView
    private var isMiniGuiaVisible = false'''
text = text.replace(vars_search, vars_replace)

find_search = 'tvChannelNumber = findViewById(R.id.tvChannelNumber)'
find_replace = '''tvChannelNumber = findViewById(R.id.tvChannelNumber)
        llMiniGuia = findViewById(R.id.llMiniGuia)
        rvMiniGuia = findViewById(R.id.rvMiniGuia)'''
text = text.replace(find_search, find_replace)

mini_guia_funcs = '''
    private fun showMiniGuia() {
        val urls = intent.getStringArrayListExtra("CHANNEL_URLS") ?: return
        val names = intent.getStringArrayListExtra("CHANNEL_NAMES") ?: return
        val covers = intent.getStringArrayListExtra("CHANNEL_COVERS")
        val currentIndex = intent.getIntExtra("CURRENT_INDEX", -1)

        isMiniGuiaVisible = true
        llMiniGuia.visibility = View.VISIBLE
        hideOverlays()

        rvMiniGuia.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.RecyclerView.HORIZONTAL, false)
        rvMiniGuia.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<MiniGuiaViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiniGuiaViewHolder {
                val view = layoutInflater.inflate(R.layout.item_mini_guia, parent, false)
                return MiniGuiaViewHolder(view)
            }
            override fun onBindViewHolder(holder: MiniGuiaViewHolder, position: Int) {
                holder.tvName.text = names[position]
                val cover = covers?.getOrNull(position)
                if (!cover.isNullOrEmpty()) {
                    com.bumptech.glide.Glide.with(this@PlayerActivity).load(cover).into(holder.ivLogo)
                } else {
                    holder.ivLogo.setImageResource(R.drawable.logo)
                }

                holder.itemView.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        holder.itemView.setBackgroundColor(android.graphics.Color.parseColor("#00E5FF"))
                        holder.tvName.setTextColor(android.graphics.Color.BLACK)
                    } else {
                        holder.itemView.setBackgroundResource(R.drawable.bg_button)
                        holder.tvName.setTextColor(android.graphics.Color.WHITE)
                    }
                }

                holder.itemView.setOnClickListener {
                    intent.putExtra("CURRENT_INDEX", position)
                    currentStreamUrl = urls[position]
                    intent.putExtra("TITLE", names[position])
                    tvLoadingTitle.text = names[position]
                    
                    hideMiniGuia()
                    playUrlInPlayer(getActivePlayer(), currentStreamUrl)
                }
            }
            override fun getItemCount() = urls.size
        }

        if (currentIndex >= 0) {
            rvMiniGuia.scrollToPosition(currentIndex)
            rvMiniGuia.postDelayed({
                rvMiniGuia.findViewHolderForAdapterPosition(currentIndex)?.itemView?.requestFocus()
            }, 100)
        }
    }

    private fun hideMiniGuia() {
        isMiniGuiaVisible = false
        llMiniGuia.visibility = View.GONE
    }

    class MiniGuiaViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val ivLogo: android.widget.ImageView = view.findViewById(R.id.ivMiniLogo)
        val tvName: android.widget.TextView = view.findViewById(R.id.tvMiniName)
    }
'''

if 'showMiniGuia' not in text:
    text = text.replace('private fun changeChannel', mini_guia_funcs + '\n    private fun changeChannel')

key_search = '''                    changeChannel(event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                    return true'''
key_replace = '''                    if (intent.getStringExtra("TYPE") == "live") {
                        if (!isMiniGuiaVisible) {
                            showMiniGuia()
                        } else {
                            hideMiniGuia()
                        }
                    } else {
                        changeChannel(event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                    }
                    return true'''
text = text.replace(key_search, key_replace)

# Ensure back button closes mini guia
back_search = 'if (isControlsVisible) {'
back_replace = '''if (isMiniGuiaVisible) {
            hideMiniGuia()
            return
        }
        if (isControlsVisible) {'''
text = text.replace(back_search, back_replace)

with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
