package com.iptv.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

class UsersActivity : AppCompatActivity() {

    private lateinit var rvUsers: RecyclerView
    private lateinit var tvEmptyUsers: TextView
    private var accountsList = mutableListOf<SavedAccount>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)

        rvUsers = findViewById(R.id.rvUsers)
        tvEmptyUsers = findViewById(R.id.tvEmptyUsers)

        rvUsers.layoutManager = GridLayoutManager(this, 3)

        findViewById<Button>(R.id.btnAddUser).setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("IS_ADDING_ACCOUNT", true)
            startActivity(intent)
        }

        loadSavedAccounts()
    }

    override fun onResume() {
        super.onResume()
        loadSavedAccounts()
    }

    private fun loadSavedAccounts() {
        accountsList.clear()
        accountsList.addAll(AccountsManager.getAccounts(this))

        if (accountsList.isEmpty()) {
            tvEmptyUsers.visibility = View.VISIBLE
            rvUsers.visibility = View.GONE
        } else {
            tvEmptyUsers.visibility = View.GONE
            rvUsers.visibility = View.VISIBLE
            rvUsers.adapter = UserCardAdapter()
        }
    }

    private fun loginAccount(account: SavedAccount) {
        Toast.makeText(this, "A iniciar sessão em ${account.username}...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiUrl = "${Constants.SERVER_URL}/player_api.php?username=${account.username}&password=${account.password}"
                val request = Request.Builder().url(apiUrl).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                var responseBody = ""
                if (response.isSuccessful) {
                    responseBody = response.body?.string() ?: ""
                }

                withContext(Dispatchers.Main) {
                    if (responseBody.contains("user_info")) {
                        var expDateFormated = account.vencimento
                        try {
                            val jsonObject = org.json.JSONObject(responseBody)
                            val userInfo = jsonObject.getJSONObject("user_info")
                            val expDateString = userInfo.getString("exp_date")
                            val timestamp = expDateString.toLong() * 1000
                            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                            expDateFormated = sdf.format(java.util.Date(timestamp))
                        } catch (e: Exception) {}

                        // Atualiza preferências e lista de contas salvas
                        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putString("USERNAME", account.username)
                            .putString("PASSWORD", account.password)
                            .apply()

                        AccountsManager.saveAccount(
                            this@UsersActivity,
                            SavedAccount(account.username, account.password, expDateFormated, System.currentTimeMillis())
                        )

                        val target = if (DeviceUtils.isTv(this@UsersActivity)) MainActivity::class.java else MobileMainActivity::class.java
                        val intent = Intent(this@UsersActivity, target)
                        intent.putExtra("VENCIMENTO", expDateFormated)
                        intent.putExtra("USERNAME", account.username)
                        intent.putExtra("PASSWORD", account.password)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@UsersActivity, "Erro no Login. Verifique os dados da conta.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UsersActivity, "Erro de conexão", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    inner class UserCardAdapter : RecyclerView.Adapter<UserCardAdapter.UserViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_card, parent, false)
            return UserViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            val acc = accountsList[position]
            holder.tvUsername.text = acc.username
            holder.tvUserVencimento.text = "Validade : ${acc.vencimento}"

            holder.btnUserLogin.setOnClickListener {
                loginAccount(acc)
            }

            holder.itemView.setOnClickListener {
                loginAccount(acc)
            }

            holder.btnUserDelete.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this@UsersActivity)
                    .setTitle("Remover Conta")
                    .setMessage("Deseja remover a conta ${acc.username} da lista?")
                    .setPositiveButton("Sim") { _, _ ->
                        AccountsManager.removeAccount(this@UsersActivity, acc.username)
                        loadSavedAccounts()
                    }
                    .setNegativeButton("Não", null)
                    .show()
            }
        }

        override fun getItemCount() = accountsList.size

        inner class UserViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvUsername: TextView = v.findViewById(R.id.tvUsername)
            val tvUserVencimento: TextView = v.findViewById(R.id.tvUserVencimento)
            val btnUserLogin: Button = v.findViewById(R.id.btnUserLogin)
            val btnUserDelete: ImageButton = v.findViewById(R.id.btnUserDelete)
        }
    }
}
