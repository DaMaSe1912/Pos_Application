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

    private fun fetchProduk() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                produkList.clear()
                displayList.clear()
                for (child in snapshot.children) {
                    val prod = child.getValue(Produk::class.java)
                    if (prod != null) {
                        produkList.add(prod)
                        val formattedHarga = formatRupiah(prod.harga)
                        displayList.add("${prod.namaProduk}\nKategori: ${prod.kategori}  |  Harga: $formattedHarga  |  Stok: ${prod.stok}")
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProdukActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
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
