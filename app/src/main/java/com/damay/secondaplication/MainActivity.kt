package com.damay.secondaplication

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import com.damay.secondaplication.akun.AkunActivity
import com.damay.secondaplication.cabang.CabangActivity
import com.damay.secondaplication.kategori.KategoriActivity
import com.damay.secondaplication.laporan.LaporanActivity
import com.damay.secondaplication.pegawai.PegawaiActivity
import com.damay.secondaplication.printer.PrinterActivity
import com.damay.secondaplication.produk.ProdukActivity
import com.damay.secondaplication.transaksi.TransaksiActivity
import com.google.firebase.database.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvBalance: TextView
    private lateinit var btnLogout: android.widget.ImageButton
    private lateinit var btnTambahSaldo: android.widget.ImageButton

    private lateinit var btnTransaksi: LinearLayout
    private lateinit var btnLaporan: LinearLayout
    private lateinit var cardAkun: CardView
    private lateinit var cardProduk: CardView
    private lateinit var cardKategori: CardView
    private lateinit var cardPegawai: CardView
    private lateinit var cardCabang: CardView
    private lateinit var cardPrinter: CardView

    private lateinit var database: FirebaseDatabase
    private lateinit var akunRef: DatabaseReference
    private lateinit var keuanganRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load applied theme from SharedPreferences before layout inflation
        val sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE)
        val savedTheme = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupFirebase()
        setupListeners()
        displayDate()
        loadDataRealtime()
    }

    private fun initViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        tvDate = findViewById(R.id.tvDate)
        tvBalance = findViewById(R.id.tvBalance)
        btnLogout = findViewById(R.id.btnLogout)
        btnTambahSaldo = findViewById(R.id.btnTambahSaldo)

        btnTransaksi = findViewById(R.id.btnTransaksi)
        btnLaporan = findViewById(R.id.btnLaporan)
        cardAkun = findViewById(R.id.cardAkun)
        cardProduk = findViewById(R.id.cardProduk)
        cardKategori = findViewById(R.id.cardKategori)
        cardPegawai = findViewById(R.id.cardPegawai)
        cardCabang = findViewById(R.id.cardCabang)
        cardPrinter = findViewById(R.id.cardPrinter)
    }

    private fun setupFirebase() {
        database = FirebaseDatabase.getInstance()
        akunRef = database.getReference("akun")
        keuanganRef = database.getReference("keuangan")
    }

    private fun setupListeners() {
        btnTransaksi.setOnClickListener {
            startActivity(Intent(this, TransaksiActivity::class.java))
        }

        btnLaporan.setOnClickListener {
            startActivity(Intent(this, LaporanActivity::class.java))
        }

        cardAkun.setOnClickListener {
            startActivity(Intent(this, AkunActivity::class.java))
        }

        cardProduk.setOnClickListener {
            startActivity(Intent(this, ProdukActivity::class.java))
        }

        cardKategori.setOnClickListener {
            startActivity(Intent(this, KategoriActivity::class.java))
        }

        cardPegawai.setOnClickListener {
            startActivity(Intent(this, PegawaiActivity::class.java))
        }

        cardCabang.setOnClickListener {
            startActivity(Intent(this, CabangActivity::class.java))
        }

        cardPrinter.setOnClickListener {
            // By default opens printer activity showing instructions/no recent receipt
            startActivity(Intent(this, PrinterActivity::class.java))
        }

        btnLogout.setOnClickListener {
            val sharedPreferences = getSharedPreferences("LoginPrefs", android.content.Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("isLoggedIn", false)
            editor.apply()
            
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnTambahSaldo.setOnClickListener {
            showTambahSaldoDialog()
        }
    }

    private fun showTambahSaldoDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Tambah Saldo")

        val input = com.google.android.material.textfield.TextInputEditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Nominal Saldo (Rp)"
        
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(48, 16, 48, 16)
        container.addView(input)
        
        builder.setView(container)

        builder.setPositiveButton("Tambah") { dialog, _ ->
            val amountStr = input.text.toString().trim()
            if (amountStr.isNotEmpty()) {
                val amount = amountStr.toDoubleOrNull()
                if (amount != null) {
                    keuanganRef.child("saldo").get().addOnSuccessListener { snapshot ->
                        val currentSaldo = if (snapshot.exists()) snapshot.getValue(Double::class.java) ?: 0.0 else 0.0
                        keuanganRef.child("saldo").setValue(currentSaldo + amount)
                        android.widget.Toast.makeText(this, "Saldo berhasil ditambahkan", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Batal") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun displayDate() {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val currentDate = sdf.format(Date())
        tvDate.text = currentDate
    }

    private fun loadDataRealtime() {
        // Read Username
        akunRef.child("username").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.value?.toString() ?: "User"
                tvGreeting.text = "${getGreetingTime()}, $username"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Read Balance
        keuanganRef.child("saldo").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val saldo = snapshot.getValue(Double::class.java) ?: 1000000.0
                    tvBalance.text = formatRupiah(saldo)
                } else {
                    // Initialize default saldo if not exists
                    keuanganRef.child("saldo").setValue(1000000.0)
                    tvBalance.text = formatRupiah(1000000.0)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun formatRupiah(number: Double): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        numberFormat.maximumFractionDigits = 0
        return numberFormat.format(number).replace("Rp", "Rp ")
    }

    private fun getGreetingTime(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        return when (hour) {
            in 0..8 -> "Selamat Pagi"
            in 9..14 -> "Selamat Siang"
            in 15..17 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }
}
