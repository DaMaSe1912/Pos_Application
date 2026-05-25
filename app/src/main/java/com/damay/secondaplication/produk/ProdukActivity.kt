package com.damay.secondaplication.produk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.damay.secondaplication.R
import com.damay.secondaplication.kategori.KategoriModel
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.*

class ProdukActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnTambah: Button
    private lateinit var databaseRef: DatabaseReference
    private lateinit var kategoriRef: DatabaseReference
    
    private lateinit var etSearchProduk: EditText
    private lateinit var btnFilterKategori: ImageButton
    
    private val produkList = ArrayList<Produk>()
    private val filteredProdukList = ArrayList<Produk>()
    val kategoriList = ArrayList<String>()
    private val selectedKategoriFilter = mutableSetOf<String>()
    private lateinit var listAdapter: ProdukAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_produk)

        listView = findViewById(R.id.listViewProduk)
        btnTambah = findViewById(R.id.btnTambahProduk)
        etSearchProduk = findViewById(R.id.etSearchProduk)
        btnFilterKategori = findViewById(R.id.btnFilterKategori)
        
        databaseRef = FirebaseDatabase.getInstance().getReference("produk")
        kategoriRef = FirebaseDatabase.getInstance().getReference("kategori")

        listAdapter = ProdukAdapter(this, filteredProdukList)
        listView.adapter = listAdapter

        btnTambah.setOnClickListener {
            startActivity(Intent(this, TambahProdukActivity::class.java))
        }

        etSearchProduk.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { filterData() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnFilterKategori.setOnClickListener { showFilterDialog() }

        fetchProduk()
        fetchKategoriList()
    }

    private fun fetchProduk() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                produkList.clear()

                if (!snapshot.exists()) {
                    Toast.makeText(this@ProdukActivity, "Belum ada data produk", Toast.LENGTH_SHORT).show()
                    listAdapter.notifyDataSetChanged()
                    return
                }

                for (child in snapshot.children) {
                    try {
                        val prod = child.getValue(Produk::class.java)
                        if (prod != null) {
                            produkList.add(prod)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseError", "Gagal konversi data: ${e.message}")
                    }
                }
                filterData()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProdukActivity, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterData() {
        val query = etSearchProduk.text.toString().trim().lowercase(Locale.getDefault())
        filteredProdukList.clear()
        for (produk in produkList) {
            val matchesQuery = produk.namaProduk!!.lowercase(Locale.getDefault()).contains(query)
            val matchesCategory = selectedKategoriFilter.isEmpty() || selectedKategoriFilter.contains(produk.kategori)
            if (matchesQuery && matchesCategory) {
                filteredProdukList.add(produk)
            }
        }
        listAdapter.notifyDataSetChanged()
    }

    private fun fetchKategoriList() {
        kategoriRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                kategoriList.clear()
                for (child in snapshot.children) {
                    val kat = child.getValue(KategoriModel::class.java)
                    if (kat != null) {
                        kategoriList.add(kat.namaKategori)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showFilterDialog() {
        if (kategoriList.isEmpty()) {
            Toast.makeText(this, "Tidak ada kategori untuk difilter", Toast.LENGTH_SHORT).show()
            return
        }

        val items = kategoriList.toTypedArray()
        val checkedItems = BooleanArray(items.size) { i -> selectedKategoriFilter.contains(items[i]) }

        AlertDialog.Builder(this)
            .setTitle("Filter Kategori")
            .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                if (isChecked) {
                    selectedKategoriFilter.add(items[which])
                } else {
                    selectedKategoriFilter.remove(items[which])
                }
            }
            .setPositiveButton("Terapkan") { dialog, _ ->
                filterData()
                dialog.dismiss()
            }
            .setNegativeButton("Reset") { dialog, _ ->
                selectedKategoriFilter.clear()
                filterData()
                dialog.dismiss()
            }
            .show()
    }

    // Custom Adapter
    private class ProdukAdapter(
        private val context: Context,
        private val list: ArrayList<Produk>
    ) : BaseAdapter() {

        override fun getCount(): Int = list.size
        override fun getItem(position: Int): Any = list[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_produk, parent, false)
            val produk = list[position]

            val tvNama = view.findViewById<TextView>(R.id.tvNamaProduk)
            val tvDetail = view.findViewById<TextView>(R.id.tvDetailProduk)
            val switchStatus = view.findViewById<SwitchCompat>(R.id.switchStatusProduk)
            val btnEdit = view.findViewById<ImageButton>(R.id.btnEditProduk)
            val btnHapus = view.findViewById<ImageButton>(R.id.btnHapusProduk)

            tvNama.text = produk.namaProduk
            val formattedHarga = formatRupiah(produk.harga)
            tvDetail.text = "${produk.kategori} | Harga: $formattedHarga | Stok: ${produk.stok}"

            switchStatus.setOnCheckedChangeListener(null)
            switchStatus.isChecked = produk.statusAktif

            switchStatus.setOnCheckedChangeListener { _, isChecked ->
                val updatedProduk = Produk(produk.id, produk.namaProduk, produk.kategori, produk.harga, produk.stok, isChecked)
                FirebaseDatabase.getInstance().getReference("produk").child(produk.id ?: "").setValue(updatedProduk)
                    .addOnFailureListener {
                        Toast.makeText(context, "Gagal mengubah status: ${it.message}", Toast.LENGTH_SHORT).show()
                        switchStatus.isChecked = !isChecked // revert
                    }
            }

            btnEdit.setOnClickListener {
                showEditPopup(produk)
            }

            btnHapus.setOnClickListener {
                showDeleteConfirmation(produk)
            }

            return view
        }

        private fun showDeleteConfirmation(produk: Produk) {
            AlertDialog.Builder(context)
                .setTitle("Hapus Produk")
                .setMessage("Apakah Anda yakin ingin menghapus produk ${produk.namaProduk}?")
                .setPositiveButton("Hapus") { dialog, _ ->
                    FirebaseDatabase.getInstance().getReference("produk").child(produk.id ?: "").removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Produk berhasil dihapus", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Gagal menghapus produk", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        private fun showEditPopup(produk: Produk) {
            val dialogBuilder = AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_produk, null)
            dialogBuilder.setView(dialogView)

            val etNama = dialogView.findViewById<TextInputEditText>(R.id.dialogEtNamaProduk)
            val etHarga = dialogView.findViewById<TextInputEditText>(R.id.dialogEtHarga)
            val etStok = dialogView.findViewById<TextInputEditText>(R.id.dialogEtStok)
            val spKategori = dialogView.findViewById<Spinner>(R.id.dialogSpKategori)

            etNama.setText(produk.namaProduk)
            
            // Format number without decimals if integer
            val hrg = if (produk.harga % 1.0 == 0.0) produk.harga.toInt().toString() else produk.harga.toString()
            etHarga.setText(hrg)
            
            etStok.setText(produk.stok.toString())

            val activity = context as ProdukActivity
            val spinnerList = ArrayList<String>()
            spinnerList.add("Pilih Kategori")
            spinnerList.addAll(activity.kategoriList)
            
            val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, spinnerList)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spKategori.adapter = spinnerAdapter

            val index = spinnerList.indexOf(produk.kategori)
            if (index >= 0) {
                spKategori.setSelection(index)
            }

            dialogBuilder.setPositiveButton("Simpan") { dialog, _ ->
                val newNama = etNama.text.toString().trim()
                val newKategori = spKategori.selectedItem.toString()
                val hargaStr = etHarga.text.toString().trim()
                val stokStr = etStok.text.toString().trim()

                if (newNama.isEmpty() || newKategori == "Pilih Kategori" || hargaStr.isEmpty() || stokStr.isEmpty()) {
                    Toast.makeText(context, "Semua input harus diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newHarga = hargaStr.toDoubleOrNull()
                val newStok = stokStr.toIntOrNull()

                if (newHarga == null || newStok == null) {
                    Toast.makeText(context, "Harga dan stok harus berupa angka valid", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedProduk = Produk(produk.id, newNama, newKategori, newHarga, newStok, produk.statusAktif)
                FirebaseDatabase.getInstance().getReference("produk").child(produk.id ?: "").setValue(updatedProduk)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Data produk berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Gagal memperbarui data produk", Toast.LENGTH_SHORT).show()
                    }
            }

            dialogBuilder.setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }

            dialogBuilder.create().show()
        }

        private fun formatRupiah(number: Double): String {
            val localeID = Locale("in", "ID")
            val numberFormat = NumberFormat.getCurrencyInstance(localeID)
            numberFormat.maximumFractionDigits = 0
            return numberFormat.format(number).replace("Rp", "Rp ")
        }
    }
}
