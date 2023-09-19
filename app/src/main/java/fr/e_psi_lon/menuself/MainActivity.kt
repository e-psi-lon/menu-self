package fr.e_psi_lon.menuself

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, NoonActivity::class.java).apply {
            putExtra("init", true)
        }
        startActivity(intent)
    }

}
