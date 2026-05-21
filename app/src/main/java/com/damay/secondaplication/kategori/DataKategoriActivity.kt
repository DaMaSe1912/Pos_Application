package com.damay.secondaplication.kategori

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.damay.secondaplication.R
import com.google.firebase.database.*

class DataKategoriActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var database: DatabaseReference
    private lateinit var btnTambah: Button
    private val listKategori = ArrayList<String>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_kategori)

        listView = findViewById(R.id.listViewKategori)
        btnTambah = findViewById(R.id.btnTambah)

        btnTambah.setOnClickListener{
            startActivity(Intent(this, ModKategoriActivity::class.java))
        }

        database = FirebaseDatabase.getInstance()
            .getReference("kategori_menu")

        ambilData()
    }

    private fun ambilData() {

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                listKategori.clear()

                for (data in snapshot.children) {
                    val namaMenu = data.child("namaMenu").value.toString()
                    val kategori = data.child("kategori").value.toString()

                    listKategori.add("$namaMenu - $kategori")
                }

                val adapter = ArrayAdapter(
                    this@DataKategoriActivity,
                    android.R.layout.simple_list_item_1,
                    listKategori
                )

                listView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}