package com.iptv.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Forçar tela cheia (Imersão Total) na Splash Screen
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        // Efeito de pulso suave na Logo
        val logo = findViewById<View>(R.id.ivSplashLogo)
        logo.alpha = 0f
        logo.animate().alpha(1f).setDuration(1000).start()
        
        logo.animate().scaleX(1.1f).scaleY(1.1f).setDuration(2000).start()

        // Aguarda 2.5 segundos e vai para a tela de Login
        CoroutineScope(Dispatchers.Main).launch {
            delay(2500)
            val intent = Intent(this@SplashActivity, LoginActivity::class.java)
            // Impede que o usuário volte para a Splash Screen apertando "Voltar"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
