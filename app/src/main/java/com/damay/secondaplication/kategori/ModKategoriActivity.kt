package com.damay.secondaplication.kategori

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.damay.secondaplication.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.FirebaseDatabase

class ModKategoriActivity : AppCompatActivity() {

    private lateinit var layoutNamaMenu: TextInputLayout
    private lateinit var etNamaMenu: TextInputEditText
    private lateinit var spKategori: Spinner
    private lateinit var btnSimpan: Button

    private val database = FirebaseDatabase.getInstance()
    private val kategoriRef = database.getReference("kategori_menu")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mod_kategori)

        initView()
        setupSpinner()
        setupButton()
    }

    private fun initView() {
        layoutNamaMenu = findViewById(R.id.layoutNamaMenu)
        etNamaMenu = findViewById(R.id.etNamaMenu)
        spKategori = findViewById(R.id.spKategori)
        btnSimpan = findViewById(R.id.btnSimpan)
    }

    private fun setupSpinner() {
        val kategoriList = listOf(
            "Pilih Kategori",
            "Makanan",
            "Minuman",
            "Snack",
            "Dessert"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            kategoriList
        )

        spKategori.adapter = adapter
    }

    private fun setupButton() {
        btnSimpan.setOnClickListener {
            if (validasiInput()) {
                simpanData()
            }
        }
    }

    private fun validasiInput(): Boolean {
        val namaMenu = etNamaMenu.text.toString().trim()
        val kategori = spKategori.selectedItem.toString()

        var valid = true

        // Validasi Nama
        if (namaMenu.isEmpty()) {
            layoutNamaMenu.error = "Nama Menu tidak boleh kosong"
            valid = false
        } else {
            layoutNamaMenu.error = null
        }

        // Validasi Spinner
        if (kategori == "Pilih Kategori") {
            Toast.makeText(this, "Silakan pilih kategori", Toast.LENGTH_SHORT).show()
            valid = false
        }

        return valid
    }

    private fun simpanData() {
        val namaMenu = etNamaMenu.text.toString().trim()
        val kategori = spKategori.selectedItem.toString()

        val id = kategoriRef.push().key
        val data = Kategori(namaMenu, kategori)

        if (id != null) {
            kategoriRef.child(id).setValue(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
                    resetForm()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun resetForm() {
        etNamaMenu.text?.clear()
        spKategori.setSelection(0)
    }
}