package com.iptv.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

import okhttp3.Request

class CategoriesActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private val categories = mutableListOf<Category>()
    private var username = ""
    private var password = ""

    private var type = "live" // default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        rvCategories = findViewById(R.id.rvCategories)
        
        // Premium grid layout for categories as well
        rvCategories.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 4)

        username = intent.getStringExtra("USERNAME") ?: ""
        password = intent.getStringExtra("PASSWORD") ?: ""
        type = intent.getStringExtra("TYPE") ?: "live"

        // Atualiza título da página
        val tvTitle = findViewById<TextView>(R.id.tvCategoryTitle) ?: return // fallback seguro
        tvTitle.text = when (type) {
            "vod" -> "FILMES"
            "series" -> "SÉRIES"
            else -> "CANAIS"
        }

        fetchCategories()
    }

    private fun fetchCategories() {
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)
        progressBar.visibility = android.view.View.VISIBLE

        val action = when (type) {
            "vod" -> "get_vod_categories"
            "series" -> "get_series_categories"
            else -> "get_live_categories"
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=$action"
                val request = Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(responseBody)

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val catId = obj.getString("category_id")
                        val catName = obj.getString("category_name")
                        val parentId = obj.optInt("parent_id", 0)
                        categories.add(Category(catId, catName, parentId))
                    }

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = android.view.View.GONE
                        rvCategories.adapter = CategoryAdapter(categories) { category ->
                            val intent = Intent(this@CategoriesActivity, StreamsActivity::class.java)
                            intent.putExtra("USERNAME", username)
                            intent.putExtra("PASSWORD", password)
                            intent.putExtra("TYPE", type)
                            intent.putExtra("CATEGORY_ID", category.category_id)
                            intent.putExtra("CATEGORY_NAME", category.category_name)
                            startActivity(intent)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this@CategoriesActivity, "Erro ao carregar categorias", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this@CategoriesActivity, "Erro de conexão", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    inner class CategoryAdapter(
        private val list: List<Category>,
        private val onClick: (Category) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvName)
            init {
                view.setOnClickListener { onClick(list[bindingAdapterPosition]) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvName.text = list[position].category_name
        }

        override fun getItemCount() = list.size
    }
}
