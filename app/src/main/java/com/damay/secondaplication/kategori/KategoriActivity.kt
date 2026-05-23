package com.damay.secondaplication.kategori

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.damay.secondaplication.R
import com.google.firebase.database.*

class KategoriActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnTambah: Button
    private lateinit var databaseRef: DatabaseReference
    
    private val categoriesList = ArrayList<KategoriModel>()
    private val displayList = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kategori)

        listView = findViewById(R.id.listViewKategori)
        btnTambah = findViewById(R.id.btnTambahKategori)
        databaseRef = FirebaseDatabase.getInstance().getReference("kategori")

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listView.adapter = adapter

        btnTambah.setOnClickListener {
            startActivity(Intent(this, TambahKategoriActivity::class.java))
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedKategori = categoriesList[position]
            showEditDialog(selectedKategori)
        }

        fetchCategories()
    }

    private fun fetchCategories() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoriesList.clear()
                displayList.clear()
                for (child in snapshot.children) {
                    val kategori = child.getValue(KategoriModel::class.java)
                    if (kategori != null) {
                        categoriesList.add(kategori)
                        displayList.add(kategori.namaKategori)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@KategoriActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showEditDialog(kategori: KategoriModel) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Ubah Nama Kategori")
        
        val input = EditText(this)
        input.setText(kategori.namaKategori)
        input.setSelection(kategori.namaKategori.length)
        dialogBuilder.setView(input)

        dialogBuilder.setPositiveButton("Simpan") { dialog, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                val updatedKategori = KategoriModel(kategori.id, newName)
                databaseRef.child(kategori.id).setValue(updatedKategori)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Kategori berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal memperbarui kategori", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Nama kategori tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.setNegativeButton("Batal") { dialog, _ ->
            dialog.cancel()
        }

        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }
}
