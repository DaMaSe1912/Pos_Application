package com.damay.secondaplication.printer

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.damay.secondaplication.R
import java.text.NumberFormat
import java.util.*

class PrinterActivity : AppCompatActivity() {

    private lateinit var tvEmptyMessage: TextView
    private lateinit var layoutReceiptContent: LinearLayout
    private lateinit var tvNotaDate: TextView
    private lateinit var tvNotaID: TextView
    private lateinit var tvNotaProductName: TextView
    private lateinit var tvNotaQtyPrice: TextView
    private lateinit var tvNotaItemSubtotal: TextView
    private lateinit var tvNotaTotal: TextView
    private lateinit var btnSelesai: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer)

        initViews()

        // Read transaction parameters from intent extras
        val transId = intent.getStringExtra("TRANS_ID")
        val prodName = intent.getStringExtra("PROD_NAME")
        val qty = intent.getIntExtra("QTY", 0)
        val priceUnit = intent.getDoubleExtra("PRICE_UNIT", 0.0)
        val priceTotal = intent.getDoubleExtra("PRICE_TOTAL", 0.0)
        val date = intent.getStringExtra("DATE")

        if (transId != null && prodName != null) {
            displayReceipt(transId, prodName, qty, priceUnit, priceTotal, date)
        } else {
            // Fetch the latest transaction from Firebase
            fetchLatestTransaction()
        }

        btnSelesai.setOnClickListener {
            finish()
        }
    }

    private fun fetchLatestTransaction() {
        val transaksiRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("transaksi")
        transaksiRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val trans = child.getValue(com.damay.secondaplication.transaksi.Transaksi::class.java)
                        if (trans != null) {
                            val unitPrice = if (trans.jumlah > 0) trans.hargaTotal / trans.jumlah else 0.0
                            displayReceipt(trans.id, trans.namaProduk, trans.jumlah, unitPrice, trans.hargaTotal, trans.tanggal)
                        }
                    }
                } else {
                    tvEmptyMessage.visibility = View.VISIBLE
                    layoutReceiptContent.visibility = View.GONE
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                tvEmptyMessage.visibility = View.VISIBLE
                tvEmptyMessage.text = "Gagal mengambil data: ${error.message}"
                layoutReceiptContent.visibility = View.GONE
            }
        })
    }

    private fun displayReceipt(transId: String, prodName: String, qty: Int, priceUnit: Double, priceTotal: Double, date: String?) {
        tvEmptyMessage.visibility = View.GONE
        layoutReceiptContent.visibility = View.VISIBLE

        tvNotaDate.text = date ?: ""
        tvNotaID.text = transId
        tvNotaProductName.text = prodName
        tvNotaQtyPrice.text = "$qty x ${formatRupiah(priceUnit)}"
        tvNotaItemSubtotal.text = formatRupiah(priceTotal)
        tvNotaTotal.text = formatRupiah(priceTotal)
    }

    private fun initViews() {
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)
        layoutReceiptContent = findViewById(R.id.layoutReceiptContent)
        tvNotaDate = findViewById(R.id.tvNotaDate)
        tvNotaID = findViewById(R.id.tvNotaID)
        tvNotaProductName = findViewById(R.id.tvNotaProductName)
        tvNotaQtyPrice = findViewById(R.id.tvNotaQtyPrice)
        tvNotaItemSubtotal = findViewById(R.id.tvNotaItemSubtotal)
        tvNotaTotal = findViewById(R.id.tvNotaTotal)
        btnSelesai = findViewById(R.id.btnSelesai)
    }

    private fun formatRupiah(number: Double): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        numberFormat.maximumFractionDigits = 0
        return numberFormat.format(number).replace("Rp", "Rp ")
    }
}
