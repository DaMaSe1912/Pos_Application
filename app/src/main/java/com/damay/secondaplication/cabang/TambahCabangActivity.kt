package com.damay.secondaplication.cabang

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.damay.secondaplication.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class TambahCabangActivity : AppCompatActivity() {

    private lateinit var etNama: TextInputEditText
    private lateinit var etAlamat: TextInputEditText
    private lateinit var btnSimpan: Button
    private lateinit var databaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_cabang)

        etNama = findViewById(R.id.etNamaCabang)
        etAlamat = findViewById(R.id.etAlamatCabang)
        btnSimpan = findViewById(R.id.btnSimpanCabang)
        databaseRef = FirebaseDatabase.getInstance().getReference("cabang")

        btnSimpan.setOnClickListener {
            simpanCabang()
        }
    }

    private fun simpanCabang() {
        val nama = etNama.text.toString().trim()
        val alamat = etAlamat.text.toString().trim()

        if (nama.isEmpty()) {
            etNama.error = "Nama cabang tidak boleh kosong"
            return
        }

        if (alamat.isEmpty()) {
            etAlamat.error = "Alamat cabang tidak boleh kosong"
            return
        }

        val id = databaseRef.push().key
        if (id != null) {
            val cabang = Cabang(id, nama, alamat)
            databaseRef.child(id).setValue(cabang)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cabang berhasil disimpan", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal menyimpan cabang", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
