package com.damay.secondaplication.transaksi

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.damay.secondaplication.R
import com.damay.secondaplication.printer.PrinterActivity
import com.damay.secondaplication.produk.Produk
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransaksiActivity : AppCompatActivity() {

    private lateinit var spProduk: Spinner
    private lateinit var etJumlahPesan: TextInputEditText
    private lateinit var tvHargaSatuan: TextView
    private lateinit var tvStokTersedia: TextView
    private lateinit var tvHargaTotal: TextView
    private lateinit var btnPesan: Button

    private lateinit var database: FirebaseDatabase
    private lateinit var produkRef: DatabaseReference
    private lateinit var keuanganRef: DatabaseReference
    private lateinit var transaksiRef: DatabaseReference

    private val productList = ArrayList<Produk>()
    private val productNames = ArrayList<String>()
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    private var selectedProduct: Produk? = null
    private var currentSaldo: Double = 1000000.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaksi)

        initViews()
        setupFirebase()
        setupListeners()
        loadProducts()
        loadSaldo()
    }

    private fun initViews() {
        spProduk = findViewById(R.id.spProduk)
        etJumlahPesan = findViewById(R.id.etJumlahPesan)
        tvHargaSatuan = findViewById(R.id.tvHargaSatuan)
        tvStokTersedia = findViewById(R.id.tvStokTersedia)
        tvHargaTotal = findViewById(R.id.tvHargaTotal)
        btnPesan = findViewById(R.id.btnPesan)
    }

    private fun setupFirebase() {
        database = FirebaseDatabase.getInstance()
        produkRef = database.getReference("produk")
        keuanganRef = database.getReference("keuangan")
        transaksiRef = database.getReference("transaksi")
    }

    private fun setupListeners() {
        // Spinner item selected listener
        spProduk.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    selectedProduct = productList[position - 1]
                    updatePriceDetails()
                } else {
                    selectedProduct = null
                    resetPriceDetails()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedProduct = null
                resetPriceDetails()
            }
        }

        // Quantity text changed listener
        etJumlahPesan.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePriceDetails()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Checkout Button Listener
        btnPesan.setOnClickListener {
            processCheckout()
        }
    }

    private fun loadProducts() {
        produkRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productList.clear()
                productNames.clear()
                productNames.add("Pilih Produk")
                for (child in snapshot.children) {
                    val prod = child.getValue(Produk::class.java)
                    if (prod != null) {
                        productList.add(prod)
                        productNames.add("${prod.namaProduk} (Stok: ${prod.stok})")
                    }
                }
                spinnerAdapter = ArrayAdapter(this@TransaksiActivity, android.R.layout.simple_spinner_item, productNames)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spProduk.adapter = spinnerAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TransaksiActivity, "Gagal memuat produk", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadSaldo() {
        keuanganRef.child("saldo").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    currentSaldo = snapshot.getValue(Double::class.java) ?: 1000000.0
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updatePriceDetails() {
        val prod = selectedProduct ?: return
        tvHargaSatuan.text = formatRupiah(prod.harga)
        tvStokTersedia.text = prod.stok.toString()

        val quantityStr = etJumlahPesan.text.toString().trim()
        val quantity = quantityStr.toIntOrNull() ?: 0

        val total = prod.harga * quantity
        tvHargaTotal.text = formatRupiah(total)
    }

    private fun resetPriceDetails() {
        tvHargaSatuan.text = "Rp 0"
        tvStokTersedia.text = "0"
        tvHargaTotal.text = "Rp 0"
    }

    private fun processCheckout() {
        val prod = selectedProduct
        if (prod == null) {
            Toast.makeText(this, "Silakan pilih produk terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val qtyStr = etJumlahPesan.text.toString().trim()
        val qty = qtyStr.toIntOrNull()

        if (qty == null || qty <= 0) {
            etJumlahPesan.error = "Jumlah pembelian harus bernilai positif"
            return
        }

        if (qty > prod.stok) {
            Toast.makeText(this, "Stok tidak mencukupi! Stok tersedia: ${prod.stok}", Toast.LENGTH_SHORT).show()
            return
        }

        val totalHarga = prod.harga * qty

        // Update database: decrease stock, decrease balance, record transaction
        val newStock = prod.stok - qty
        val newSaldo = currentSaldo - totalHarga

        val transactionId = transaksiRef.push().key
        if (transactionId != null) {
            val sdf = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
            val currentDate = sdf.format(Date())

            val trans = Transaksi(transactionId, prod.namaProduk, qty, totalHarga, currentDate)

            // Perform batch update to keep database in sync
            val updates = HashMap<String, Any>()
            updates["/produk/${prod.id}/stok"] = newStock
            updates["/keuangan/saldo"] = newSaldo
            updates["/transaksi/$transactionId"] = trans

            database.reference.updateChildren(updates).addOnSuccessListener {
                Toast.makeText(this, "Pemesanan berhasil dilakukan!", Toast.LENGTH_SHORT).show()
                
                // Open PrinterActivity dynamically passing details
                val intent = Intent(this, PrinterActivity::class.java).apply {
                    putExtra("TRANS_ID", transactionId)
                    putExtra("PROD_NAME", prod.namaProduk)
                    putExtra("QTY", qty)
                    putExtra("PRICE_UNIT", prod.harga)
                    putExtra("PRICE_TOTAL", totalHarga)
                    putExtra("DATE", currentDate)
                }
                startActivity(intent)
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Gagal memproses pemesanan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatRupiah(number: Double): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        numberFormat.maximumFractionDigits = 0
        return numberFormat.format(number).replace("Rp", "Rp ")
    }
}
