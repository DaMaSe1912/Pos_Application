package com.damay.secondaplication.cabang

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.damay.secondaplication.R
import com.damay.secondaplication.pegawai.Pegawai
import com.google.firebase.database.*

class CabangActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnTambah: Button
    private lateinit var cabangRef: DatabaseReference
    private lateinit var pegawaiRef: DatabaseReference

    private val cabangList = ArrayList<Cabang>()
    private val pegawaiList = ArrayList<Pegawai>()
    private val displayList = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cabang)

        listView = findViewById(R.id.listViewCabang)
        btnTambah = findViewById(R.id.btnTambahCabang)

        cabangRef = FirebaseDatabase.getInstance().getReference("cabang")
        pegawaiRef = FirebaseDatabase.getInstance().getReference("pegawai")

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listView.adapter = adapter

        btnTambah.setOnClickListener {
            startActivity(Intent(this, TambahCabangActivity::class.java))
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedCabang = cabangList[position]
            showPegawaiDialog(selectedCabang)
        }

        fetchCabangAndPegawai()
    }

    private fun fetchCabangAndPegawai() {
        // Read employees
        pegawaiRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                pegawaiList.clear()
                for (child in snapshot.children) {
                    val peg = child.getValue(Pegawai::class.java)
                    if (peg != null) {
                        pegawaiList.add(peg)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Read branches
        cabangRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cabangList.clear()
                displayList.clear()
                for (child in snapshot.children) {
                    val cab = child.getValue(Cabang::class.java)
                    if (cab != null) {
                        cabangList.add(cab)
                        displayList.add("${cab.namaCabang}\nAlamat: ${cab.alamatCabang}")
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CabangActivity, "Gagal mengambil data cabang", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showPegawaiDialog(cabang: Cabang) {
        val filteredPegawai = pegawaiList.filter { it.cabang == cabang.namaCabang }

        val message = StringBuilder()
        if (filteredPegawai.isEmpty()) {
            message.append("Tidak ada pegawai yang ditempatkan di cabang ini.")
        } else {
            message.append("Daftar Pegawai Aktif:\n\n")
            filteredPegawai.forEachIndexed { index, peg ->
                message.append("${index + 1}. ${peg.nama}\n   Email: ${peg.email}\n   Telp: ${peg.telepon}\n\n")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Cabang ${cabang.namaCabang}")
            .setMessage(message.toString())
            .setPositiveButton("Tutup") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
