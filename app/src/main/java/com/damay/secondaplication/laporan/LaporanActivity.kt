package com.damay.secondaplication.laporan

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.damay.secondaplication.R
import com.damay.secondaplication.printer.PrinterActivity
import com.damay.secondaplication.transaksi.Transaksi
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.*

class LaporanActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var databaseRef: DatabaseReference

    private val transaksiList = ArrayList<Transaksi>()
    private val displayList = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laporan)

        listView = findViewById(R.id.listViewLaporan)
        databaseRef = FirebaseDatabase.getInstance().getReference("transaksi")

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedTrans = transaksiList[position]
            reprintReceipt(selectedTrans)
        }

        fetchLaporan()
    }

    private fun fetchLaporan() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transaksiList.clear()
                displayList.clear()
                
                // Read transactions and reverse list to show latest first
                val tempList = ArrayList<Transaksi>()
                for (child in snapshot.children) {
                    val trans = child.getValue(Transaksi::class.java)
                    if (trans != null) {
                        tempList.add(trans)
                    }
                }
                
                tempList.reverse()
                transaksiList.addAll(tempList)

                for (trans in transaksiList) {
                    val formattedTotal = formatRupiah(trans.hargaTotal)
                    displayList.add("${trans.namaProduk}  (x${trans.jumlah})\nTotal: $formattedTotal  |  ${trans.tanggal}")
                }
                
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LaporanActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun reprintReceipt(trans: Transaksi) {
        val priceUnit = if (trans.jumlah > 0) trans.hargaTotal / trans.jumlah else trans.hargaTotal
        
        val intent = Intent(this, PrinterActivity::class.java).apply {
            putExtra("TRANS_ID", trans.id)
            putExtra("PROD_NAME", trans.namaProduk)
            putExtra("QTY", trans.jumlah)
            putExtra("PRICE_UNIT", priceUnit)
            putExtra("PRICE_TOTAL", trans.hargaTotal)
            putExtra("DATE", trans.tanggal)
        }
        startActivity(intent)
    }

    private fun formatRupiah(number: Double): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        numberFormat.maximumFractionDigits = 0
        return numberFormat.format(number).replace("Rp", "Rp ")
    }
}
