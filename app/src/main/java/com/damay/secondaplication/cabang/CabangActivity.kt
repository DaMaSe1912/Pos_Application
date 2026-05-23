package com.damay.secondaplication.cabang

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.damay.secondaplication.R
import com.damay.secondaplication.pegawai.Pegawai
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*

class CabangActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnTambah: Button
    private lateinit var cabangRef: DatabaseReference
    private lateinit var pegawaiRef: DatabaseReference

    private val cabangList = ArrayList<Cabang>()
    private val pegawaiList = ArrayList<Pegawai>()
    private lateinit var listAdapter: CabangAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cabang)

        listView = findViewById(R.id.listViewCabang)
        btnTambah = findViewById(R.id.btnTambahCabang)

        cabangRef = FirebaseDatabase.getInstance().getReference("cabang")
        pegawaiRef = FirebaseDatabase.getInstance().getReference("pegawai")

        listAdapter = CabangAdapter(this, cabangList, pegawaiList)
        listView.adapter = listAdapter

        btnTambah.setOnClickListener {
            startActivity(Intent(this, TambahCabangActivity::class.java))
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
                for (child in snapshot.children) {
                    val cab = child.getValue(Cabang::class.java)
                    if (cab != null) {
                        cabangList.add(cab)
                    }
                }
                listAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CabangActivity, "Gagal mengambil data cabang", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Custom Adapter
    private class CabangAdapter(
        private val context: Context,
        private val list: ArrayList<Cabang>,
        private val pegawaiList: ArrayList<Pegawai>
    ) : BaseAdapter() {

        override fun getCount(): Int = list.size
        override fun getItem(position: Int): Any = list[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_cabang, parent, false)
            val cabang = list[position]

            val tvNama = view.findViewById<TextView>(R.id.tvNamaCabangItem)
            val tvAlamat = view.findViewById<TextView>(R.id.tvAlamatCabangItem)
            val btnEdit = view.findViewById<ImageButton>(R.id.btnEditCabang)
            val btnHapus = view.findViewById<ImageButton>(R.id.btnHapusCabang)

            tvNama.text = cabang.namaCabang
            tvAlamat.text = cabang.alamatCabang

            // Handle klik item (card) untuk menampilkan daftar pegawai
            view.setOnClickListener {
                showPegawaiDialog(cabang)
            }

            btnEdit.setOnClickListener {
                showEditPopup(cabang)
            }

            btnHapus.setOnClickListener {
                showDeleteConfirmation(cabang)
            }

            return view
        }

        private fun showPegawaiDialog(cabang: Cabang) {
            val filteredPegawai = pegawaiList.filter { it.cabang == cabang.namaCabang }

            val message = java.lang.StringBuilder()
            if (filteredPegawai.isEmpty()) {
                message.append("Tidak ada pegawai yang ditempatkan di cabang ini.")
            } else {
                message.append("Daftar Pegawai Aktif:\n\n")
                filteredPegawai.forEachIndexed { index, peg ->
                    message.append("${index + 1}. ${peg.nama}\n   Email: ${peg.email}\n   Telp: ${peg.telepon}\n\n")
                }
            }

            AlertDialog.Builder(context)
                .setTitle("Cabang ${cabang.namaCabang}")
                .setMessage(message.toString())
                .setPositiveButton("Tutup") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        private fun showEditPopup(cabang: Cabang) {
            val dialogBuilder = AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_cabang, null)
            dialogBuilder.setView(dialogView)

            val etNama = dialogView.findViewById<TextInputEditText>(R.id.dialogEtNamaCabang)
            val etAlamat = dialogView.findViewById<TextInputEditText>(R.id.dialogEtAlamatCabang)

            etNama.setText(cabang.namaCabang)
            etAlamat.setText(cabang.alamatCabang)

            dialogBuilder.setPositiveButton("Simpan") { dialog, _ ->
                val newNama = etNama.text.toString().trim()
                val newAlamat = etAlamat.text.toString().trim()

                if (newNama.isEmpty() || newAlamat.isEmpty()) {
                    Toast.makeText(context, "Semua input harus diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedCabang = Cabang(cabang.id, newNama, newAlamat)
                FirebaseDatabase.getInstance().getReference("cabang").child(cabang.id).setValue(updatedCabang)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Data cabang berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Gagal memperbarui cabang", Toast.LENGTH_SHORT).show()
                    }
            }

            dialogBuilder.setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }

            dialogBuilder.create().show()
        }

        private fun showDeleteConfirmation(cabang: Cabang) {
            AlertDialog.Builder(context)
                .setTitle("Hapus Cabang")
                .setMessage("Apakah Anda yakin ingin menghapus cabang ${cabang.namaCabang}?")
                .setPositiveButton("Hapus") { dialog, _ ->
                    FirebaseDatabase.getInstance().getReference("cabang").child(cabang.id).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Cabang berhasil dihapus", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Gagal menghapus cabang", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
}
