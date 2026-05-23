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
                tvGreeting.text = "Selamat Siang, $username"
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
}
