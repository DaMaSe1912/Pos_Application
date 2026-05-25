package com.damay.secondaplication.kategori

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
import com.google.firebase.database.*

class KategoriActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnTambah: Button
    private lateinit var databaseRef: DatabaseReference
    
    private val categoriesList = ArrayList<KategoriModel>()
    private lateinit var listAdapter: KategoriAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kategori)

        listView = findViewById(R.id.listViewKategori)
        btnTambah = findViewById(R.id.btnTambahKategori)
        databaseRef = FirebaseDatabase.getInstance().getReference("kategori")

        listAdapter = KategoriAdapter(this, categoriesList)
        listView.adapter = listAdapter

        btnTambah.setOnClickListener {
            startActivity(Intent(this, TambahKategoriActivity::class.java))
        }

        fetchCategories()
    }

    private fun fetchCategories() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoriesList.clear()
                for (child in snapshot.children) {
                    val kategori = child.getValue(KategoriModel::class.java)
                    if (kategori != null) {
                        categoriesList.add(kategori)
                    }
                }
                listAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@KategoriActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Custom Adapter
    private class KategoriAdapter(
        private val context: Context,
        private val list: ArrayList<KategoriModel>
    ) : BaseAdapter() {

        override fun getCount(): Int = list.size
        override fun getItem(position: Int): Any = list[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_kategori, parent, false)
            val kategori = list[position]

            val tvNama = view.findViewById<TextView>(R.id.tvNamaKategoriItem)
            val switchStatus = view.findViewById<SwitchCompat>(R.id.switchStatusKategori)
            val btnEdit = view.findViewById<ImageButton>(R.id.btnEditKategori)
            val btnHapus = view.findViewById<ImageButton>(R.id.btnHapusKategori)

            tvNama.text = kategori.namaKategori

            switchStatus.setOnCheckedChangeListener(null)
            switchStatus.isChecked = kategori.statusAktif

            switchStatus.setOnCheckedChangeListener { _, isChecked ->
                val updatedKategori = KategoriModel(kategori.id, kategori.namaKategori, isChecked)
                FirebaseDatabase.getInstance().getReference("kategori").child(kategori.id).setValue(updatedKategori)
                    .addOnFailureListener {
                        Toast.makeText(context, "Gagal mengubah status: ${it.message}", Toast.LENGTH_SHORT).show()
                        switchStatus.isChecked = !isChecked // revert
                    }
            }

            btnEdit.setOnClickListener {
                showEditDialog(kategori)
            }

            btnHapus.setOnClickListener {
                showDeleteConfirmation(kategori)
            }

            return view
        }

        private fun showDeleteConfirmation(kategori: KategoriModel) {
            AlertDialog.Builder(context)
                .setTitle("Hapus Kategori")
                .setMessage("Apakah Anda yakin ingin menghapus kategori '${kategori.namaKategori}'? Peringatan: Semua produk dalam kategori ini juga akan terhapus!")
                .setPositiveButton("Hapus") { dialog, _ ->
                    val database = FirebaseDatabase.getInstance()
                    
                    // 1. Hapus Kategori
                    database.getReference("kategori").child(kategori.id).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Kategori berhasil dihapus", Toast.LENGTH_SHORT).show()
                            
                            // 2. Hapus Produk Terkait (Cascading Delete)
                            val produkRef = database.getReference("produk")
                            produkRef.orderByChild("kategori").equalTo(kategori.namaKategori)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        for (child in snapshot.children) {
                                            child.ref.removeValue()
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {
                                        Toast.makeText(context, "Gagal menghapus produk terkait", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Gagal menghapus kategori", Toast.LENGTH_SHORT).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        private fun showEditDialog(kategori: KategoriModel) {
            val dialogBuilder = AlertDialog.Builder(context)
            dialogBuilder.setTitle("Ubah Nama Kategori")
            
            val input = EditText(context)
            input.setText(kategori.namaKategori)
            input.setSelection(kategori.namaKategori.length)
            dialogBuilder.setView(input)

            dialogBuilder.setPositiveButton("Simpan") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val updatedKategori = KategoriModel(kategori.id, newName, kategori.statusAktif)
                    FirebaseDatabase.getInstance().getReference("kategori").child(kategori.id).setValue(updatedKategori)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Kategori berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Gagal memperbarui kategori", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Nama kategori tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }

            dialogBuilder.setNegativeButton("Batal") { dialog, _ ->
                dialog.cancel()
            }

            val alertDialog = dialogBuilder.create()
            alertDialog.show()
        }
    }
}
