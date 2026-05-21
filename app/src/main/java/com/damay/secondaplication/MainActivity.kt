package com.damay.secondaplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.damay.secondaplication.kategori.DataKategoriActivity

class MainActivity : AppCompatActivity() {

    private lateinit var cardKategori: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cardKategori = findViewById(R.id.cardKategori)

        cardKategori.setOnClickListener {
            startActivity(
                Intent(this, DataKategoriActivity::class.java)
            )
        }
    }
}
