package com.damay.secondaplication.produk

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.damay.secondaplication.R
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.*

class ProdukActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnTambah: Button
    private lateinit var databaseRef: DatabaseReference
    
    private val produkList = ArrayList<Produk>()
    private val displayList = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_produk)

        listView = findViewById(R.id.listViewProduk)
        btnTambah = findViewById(R.id.btnTambahProduk)
        databaseRef = FirebaseDatabase.getInstance().getReference("produk")

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listView.adapter = adapter

        btnTambah.setOnClickListener {
            startActivity(Intent(this, TambahProdukActivity::class.java))
        }

        fetchProduk()
    }

    // ... (kode lainnya tetap sama)

    private fun fetchProduk() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                produkList.clear()
                displayList.clear()

                if (!snapshot.exists()) {
                    Toast.makeText(this@ProdukActivity, "Belum ada data produk", Toast.LENGTH_SHORT).show()
                    adapter.notifyDataSetChanged()
                    return
                }

                for (child in snapshot.children) {
                    try {
                        val prod = child.getValue(Produk::class.java)
                        if (prod != null) {
                            produkList.add(prod)
                            val formattedHarga = formatRupiah(prod.harga)
                            // Menggunakan String template untuk tampilan List
                            displayList.add("${prod.namaProduk}\n${prod.kategori} | Harga: $formattedHarga | Stok: ${prod.stok}")
                        }
                    } catch (e: Exception) {
                        // Ini akan muncul di Logcat jika tipe data di Firebase tidak sesuai dengan Model Produk
                        android.util.Log.e("FirebaseError", "Gagal konversi data: ${e.message}")
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProdukActivity, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun formatRupiah(number: Double): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        numberFormat.maximumFractionDigits = 0
        return numberFormat.format(number).replace("Rp", "Rp ")
    }
}
