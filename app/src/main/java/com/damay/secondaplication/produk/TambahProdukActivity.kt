package com.damay.secondaplication.produk

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.damay.secondaplication.R
import com.damay.secondaplication.kategori.KategoriModel
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*

class TambahProdukActivity : AppCompatActivity() {

    private lateinit var etNamaProduk: TextInputEditText
    private lateinit var spKategori: Spinner
    private lateinit var etHargaProduk: TextInputEditText
    private lateinit var etStokProduk: TextInputEditText
    private lateinit var btnSimpan: Button

    private lateinit var databaseRef: DatabaseReference
    private lateinit var kategoriRef: DatabaseReference

    private val kategoriList = ArrayList<String>()
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_produk)

        etNamaProduk = findViewById(R.id.etNamaProduk)
        spKategori = findViewById(R.id.spKategori)
        etHargaProduk = findViewById(R.id.etHargaProduk)
        etStokProduk = findViewById(R.id.etStokProduk)
        btnSimpan = findViewById(R.id.btnSimpanProduk)

        databaseRef = FirebaseDatabase.getInstance().getReference("produk")
        kategoriRef = FirebaseDatabase.getInstance().getReference("kategori")

        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kategoriList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spKategori.adapter = spinnerAdapter

        btnSimpan.setOnClickListener {
            simpanProduk()
        }

        loadCategories()
    }

    private fun loadCategories() {
        kategoriRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                kategoriList.clear()
                kategoriList.add("Pilih Kategori")
                for (child in snapshot.children) {
                    val kategori = child.getValue(KategoriModel::class.java)
                    if (kategori != null) {
                        kategoriList.add(kategori.namaKategori)
                    }
                }
                spinnerAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TambahProdukActivity, "Gagal memuat kategori", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun simpanProduk() {
        val nama = etNamaProduk.text.toString().trim()
        val kategori = spKategori.selectedItem.toString()
        val hargaStr = etHargaProduk.text.toString().trim()
        val stokStr = etStokProduk.text.toString().trim()

        if (nama.isEmpty()) {
            etNamaProduk.error = "Nama produk tidak boleh kosong"
            return
        }

        if (kategori == "Pilih Kategori") {
            Toast.makeText(this, "Silakan pilih kategori produk", Toast.LENGTH_SHORT).show()
            return
        }

        if (hargaStr.isEmpty()) {
            etHargaProduk.error = "Harga produk tidak boleh kosong"
            return
        }

        if (stokStr.isEmpty()) {
            etStokProduk.error = "Stok awal tidak boleh kosong"
            return
        }

        val harga = hargaStr.toDoubleOrNull()
        val stok = stokStr.toIntOrNull()

        if (harga == null || harga <= 0) {
            etHargaProduk.error = "Harga harus bernilai positif"
            return
        }

        if (stok == null || stok < 0) {
            etStokProduk.error = "Stok tidak boleh negatif"
            return
        }

        val id = databaseRef.push().key
        if (id != null) {
            val produk = Produk(id, nama, kategori, harga, stok)
            databaseRef.child(id).setValue(produk)
                .addOnSuccessListener {
                    Toast.makeText(this, "Produk berhasil disimpan", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal menyimpan produk", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
