package com.damay.secondaplication.kategori

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.damay.secondaplication.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class TambahKategoriActivity : AppCompatActivity() {

    private lateinit var etNamaKategori: TextInputEditText
    private lateinit var btnSimpan: Button
    private lateinit var databaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_kategori)

        etNamaKategori = findViewById(R.id.etNamaKategori)
        btnSimpan = findViewById(R.id.btnSimpanKategori)
        databaseRef = FirebaseDatabase.getInstance().getReference("kategori")

        btnSimpan.setOnClickListener {
            simpanKategori()
        }
    }

    private fun simpanKategori() {
        val namaKategori = etNamaKategori.text.toString().trim()

        if (namaKategori.isEmpty()) {
            etNamaKategori.error = "Nama kategori tidak boleh kosong"
            return
        }

        val id = databaseRef.push().key
        if (id != null) {
            val kategori = KategoriModel(id, namaKategori)
            databaseRef.child(id).setValue(kategori)
                .addOnSuccessListener {
                    Toast.makeText(this, "Kategori berhasil disimpan", Toast.LENGTH_SHORT).show()
                    finish() // Close activity
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal menyimpan kategori", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
