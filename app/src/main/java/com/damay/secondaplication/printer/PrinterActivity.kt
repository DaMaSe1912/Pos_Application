package com.damay.secondaplication.printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.damay.secondaplication.R
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.Charset
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

    // Bluetooth UI components
    private lateinit var spinnerPrinters: Spinner
    private lateinit var btnRefreshPrinters: ImageButton
    private lateinit var tvPrinterStatus: TextView
    private lateinit var btnPrintReceipt: Button

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val pairedDevices = ArrayList<BluetoothDevice>()

    // Member variables to cache transaction data for physical printing
    private var transId: String? = null
    private var prodName: String? = null
    private var qty: Int = 0
    private var priceUnit: Double = 0.0
    private var priceTotal: Double = 0.0
    private var date: String? = null

    private val PERMISSION_BLUETOOTH_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer)

        initViews()

        // Read transaction parameters from intent extras
        val transIdExtra = intent.getStringExtra("TRANS_ID")
        val prodNameExtra = intent.getStringExtra("PROD_NAME")
        val qtyExtra = intent.getIntExtra("QTY", 0)
        val priceUnitExtra = intent.getDoubleExtra("PRICE_UNIT", 0.0)
        val priceTotalExtra = intent.getDoubleExtra("PRICE_TOTAL", 0.0)
        val dateExtra = intent.getStringExtra("DATE")

        if (transIdExtra != null && prodNameExtra != null) {
            displayReceipt(transIdExtra, prodNameExtra, qtyExtra, priceUnitExtra, priceTotalExtra, dateExtra)
        } else {
            // Fetch the latest transaction from Firebase
            fetchLatestTransaction()
        }

        // Initialize Bluetooth configuration and load paired devices
        checkBluetoothAndLoadDevices()

        btnRefreshPrinters.setOnClickListener {
            checkBluetoothAndLoadDevices()
        }

        btnPrintReceipt.setOnClickListener {
            triggerPrint()
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
        this.transId = transId
        this.prodName = prodName
        this.qty = qty
        this.priceUnit = priceUnit
        this.priceTotal = priceTotal
        this.date = date

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

        // Bluetooth UI bindings
        spinnerPrinters = findViewById(R.id.spinnerPrinters)
        btnRefreshPrinters = findViewById(R.id.btnRefreshPrinters)
        tvPrinterStatus = findViewById(R.id.tvPrinterStatus)
        btnPrintReceipt = findViewById(R.id.btnPrintReceipt)
    }

    private fun checkBluetoothAndLoadDevices() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            tvPrinterStatus.text = "Status: Bluetooth tidak didukung"
            tvPrinterStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
            Toast.makeText(this, "Perangkat ini tidak mendukung Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            tvPrinterStatus.text = "Status: Bluetooth tidak aktif"
            tvPrinterStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
            Toast.makeText(this, "Silakan aktifkan Bluetooth terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_BLUETOOTH_REQUEST_CODE)
                return
            }
        }

        loadPairedDevices()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_BLUETOOTH_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadPairedDevices()
            } else {
                tvPrinterStatus.text = "Status: Izin Bluetooth Ditolak"
                tvPrinterStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
                Toast.makeText(this, "Izin akses Bluetooth diperlukan untuk mencetak struk fisik", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadPairedDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val bonded = bluetoothAdapter?.bondedDevices ?: emptySet()
        pairedDevices.clear()
        val deviceNames = ArrayList<String>()

        if (bonded.isEmpty()) {
            deviceNames.add("Tidak ada printer terpasang (paired)")
            tvPrinterStatus.text = "Status: Pasangkan printer di pengaturan Bluetooth"
            tvPrinterStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
        } else {
            deviceNames.add("Pilih Printer Bluetooth")
            for (device in bonded) {
                pairedDevices.add(device)
                deviceNames.add("${device.name} (${device.address})")
            }
            tvPrinterStatus.text = "Status: Siap menghubungkan"
            tvPrinterStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPrinters.adapter = adapter
    }

    private fun triggerPrint() {
        if (transId == null || prodName == null) {
            Toast.makeText(this, "Data transaksi belum siap", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedIndex = spinnerPrinters.selectedItemPosition
        if (selectedIndex <= 0 || selectedIndex > pairedDevices.size) {
            Toast.makeText(this, "Silakan pilih printer Bluetooth terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val device = pairedDevices[selectedIndex - 1]
        val printBytes = generateEscPosBytes()
        connectAndPrint(device, printBytes)
    }

    private fun generateEscPosBytes(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        try {
            val ESC = 0x1B.toByte()

            val ESC_INIT = byteArrayOf(ESC, 0x40)
            val ESC_ALIGN_LEFT = byteArrayOf(ESC, 0x61, 0x00)
            val ESC_ALIGN_CENTER = byteArrayOf(ESC, 0x61, 0x01)

            val ESC_TEXT_NORMAL = byteArrayOf(ESC, 0x21, 0x00)
            val ESC_TEXT_BOLD = byteArrayOf(ESC, 0x21, 0x08)
            val ESC_TEXT_DOUBLE = byteArrayOf(ESC, 0x21, 0x38) // Bold + double height + double width

            // Initialize printer
            outputStream.write(ESC_INIT)

            // Header: D&M POS SYSTEM (Center, Bold, Double Size)
            outputStream.write(ESC_ALIGN_CENTER)
            outputStream.write(ESC_TEXT_DOUBLE)
            outputStream.write("D&M POS SYSTEM\n".toByteArray(Charset.defaultCharset()))

            // Subheader: Cabang Pusat Digital (Center, Normal)
            outputStream.write(ESC_TEXT_NORMAL)
            outputStream.write("Cabang Pusat Digital\n".toByteArray(Charset.defaultCharset()))
            outputStream.write("================================\n".toByteArray(Charset.defaultCharset()))

            // Metadata: Left alignment
            outputStream.write(ESC_ALIGN_LEFT)
            outputStream.write("Tanggal: ${date ?: ""}\n".toByteArray(Charset.defaultCharset()))
            outputStream.write("No Nota: $transId\n".toByteArray(Charset.defaultCharset()))
            outputStream.write("--------------------------------\n".toByteArray(Charset.defaultCharset()))

            // Item Name (Bold)
            outputStream.write(ESC_TEXT_BOLD)
            outputStream.write("$prodName\n".toByteArray(Charset.defaultCharset()))

            // Item Price: normal text, right/left aligned
            outputStream.write(ESC_TEXT_NORMAL)
            val qtyPrice = "$qty x ${formatRupiah(priceUnit)}"
            val subTotal = formatRupiah(priceTotal)
            val itemLine = formatLine(qtyPrice, subTotal, 32)
            outputStream.write("$itemLine\n".toByteArray(Charset.defaultCharset()))
            outputStream.write("--------------------------------\n".toByteArray(Charset.defaultCharset()))

            // Total
            outputStream.write(ESC_TEXT_BOLD)
            val totalLine = formatLine("TOTAL BELANJA:", formatRupiah(priceTotal), 32)
            outputStream.write("$totalLine\n".toByteArray(Charset.defaultCharset()))
            outputStream.write(ESC_TEXT_NORMAL)
            outputStream.write("================================\n".toByteArray(Charset.defaultCharset()))

            // Footer
            outputStream.write(ESC_ALIGN_CENTER)
            outputStream.write("Terima kasih atas\n".toByteArray(Charset.defaultCharset()))
            outputStream.write("kunjungan Anda!\n".toByteArray(Charset.defaultCharset()))
            outputStream.write("Layanan Konsumen:\n".toByteArray(Charset.defaultCharset()))
            outputStream.write("0812-3456-789\n".toByteArray(Charset.defaultCharset()))

            // Feed paper
            outputStream.write("\n\n\n\n".toByteArray(Charset.defaultCharset()))

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return outputStream.toByteArray()
    }

    private fun formatLine(left: String, right: String, totalWidth: Int): String {
        val spacesNeeded = totalWidth - left.length - right.length
        return if (spacesNeeded > 0) {
            left + " ".repeat(spacesNeeded) + right
        } else {
            "$left $right"
        }
    }

    private fun connectAndPrint(device: BluetoothDevice, printBytes: ByteArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Izin Bluetooth tidak diberikan", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceName = device.name ?: "Printer"
        tvPrinterStatus.text = "Status: Menghubungkan ke $deviceName..."
        tvPrinterStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_light))

        Thread {
            var socket: BluetoothSocket? = null
            try {
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard Serial SPP UUID
                socket = device.createRfcommSocketToServiceRecord(uuid)
                socket.connect()

                runOnUiThread {
                    tvPrinterStatus.text = "Status: Mencetak ke $deviceName..."
                    tvPrinterStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
                }

                val outputStream = socket.outputStream
                outputStream.write(printBytes)
                outputStream.flush()

                // Wait a bit to ensure bytes are transmitted
                Thread.sleep(1000)

                runOnUiThread {
                    tvPrinterStatus.text = "Status: Selesai Mencetak"
                    tvPrinterStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
                    Toast.makeText(this@PrinterActivity, "Nota berhasil dicetak!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    tvPrinterStatus.text = "Status: Gagal menghubungkan/mencetak"
                    tvPrinterStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
                    Toast.makeText(this@PrinterActivity, "Gagal mencetak: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                try {
                    socket?.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            }
        }.start()
    }

    private fun formatRupiah(number: Double): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        numberFormat.maximumFractionDigits = 0
        return numberFormat.format(number).replace("Rp", "Rp ")
    }
}
