package com.damay.secondaplication.pegawai

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
import com.damay.secondaplication.cabang.Cabang
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*

class PegawaiActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnTambah: Button
    private lateinit var databaseRef: DatabaseReference
    private lateinit var cabangRef: DatabaseReference

    private val pegawaiList = ArrayList<Pegawai>()
    private val cabangList = ArrayList<String>()
    private lateinit var listAdapter: PegawaiAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pegawai)

        listView = findViewById(R.id.listViewPegawai)
        btnTambah = findViewById(R.id.btnTambahPegawai)

        databaseRef = FirebaseDatabase.getInstance().getReference("pegawai")
        cabangRef = FirebaseDatabase.getInstance().getReference("cabang")

        listAdapter = PegawaiAdapter(this, pegawaiList)
        listView.adapter = listAdapter

        btnTambah.setOnClickListener {
            startActivity(Intent(this, TambahPegawaiActivity::class.java))
        }

        fetchPegawai()
        fetchCabangList()
    }

    private fun fetchPegawai() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                pegawaiList.clear()
                for (child in snapshot.children) {
                    val peg = child.getValue(Pegawai::class.java)
                    if (peg != null) {
                        pegawaiList.add(peg)
                    }
                }
                listAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PegawaiActivity, "Gagal mengambil data pegawai", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchCabangList() {
        cabangRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cabangList.clear()
                cabangList.add("Pilih Cabang")
                for (child in snapshot.children) {
                    val cab = child.getValue(Cabang::class.java)
                    if (cab != null) {
                        cabangList.add(cab.namaCabang)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Custom Adapter class inside PegawaiActivity for employee items
    private class PegawaiAdapter(
        private val context: Context,
        private val list: ArrayList<Pegawai>
    ) : BaseAdapter() {

        override fun getCount(): Int = list.size
        override fun getItem(position: Int): Any = list[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_pegawai, parent, false)
            val pegawai = list[position]

            val tvNama = view.findViewById<TextView>(R.id.tvNamaPegawai)
            val tvDetail = view.findViewById<TextView>(R.id.tvDetailPegawai)
            val btnEdit = view.findViewById<ImageButton>(R.id.btnEditPegawai)
            val btnHapus = view.findViewById<ImageButton>(R.id.btnHapusPegawai)

            tvNama.text = pegawai.nama
            tvDetail.text = "${pegawai.email}  |  ${pegawai.telepon}\nCabang: ${pegawai.cabang}"

            btnHapus.setOnClickListener {
                showDeleteConfirmation(pegawai)
            }

            btnEdit.setOnClickListener {
                showEditPopup(pegawai)
            }

            return view
        }

        private fun showDeleteConfirmation(pegawai: Pegawai) {
            AlertDialog.Builder(context)
                .setTitle("Hapus Pegawai")
                .setMessage("Apakah Anda yakin ingin menghapus data ${pegawai.nama}?")
                .setPositiveButton("Hapus") { dialog, _ ->
                    FirebaseDatabase.getInstance().getReference("pegawai").child(pegawai.id).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Pegawai berhasil dihapus", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Gagal menghapus pegawai", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        private fun showEditPopup(pegawai: Pegawai) {
            val dialogBuilder = AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_pegawai, null)
            dialogBuilder.setView(dialogView)

            val etNama = dialogView.findViewById<TextInputEditText>(R.id.dialogEtNama)
            val etEmail = dialogView.findViewById<TextInputEditText>(R.id.dialogEtEmail)
            val etTelepon = dialogView.findViewById<TextInputEditText>(R.id.dialogEtTelepon)
            val spCabang = dialogView.findViewById<Spinner>(R.id.dialogSpCabang)

            // Prefill current data
            etNama.setText(pegawai.nama)
            etEmail.setText(pegawai.email)
            etTelepon.setText(pegawai.telepon)

            // Setup Cabang spinner options
            val activity = context as PegawaiActivity
            val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, activity.cabangList)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spCabang.adapter = spinnerAdapter

            // Select active branch if match found
            val index = activity.cabangList.indexOf(pegawai.cabang)
            if (index >= 0) {
                spCabang.setSelection(index)
            }

            dialogBuilder.setPositiveButton("Simpan") { dialog, _ ->
                val newNama = etNama.text.toString().trim()
                val newEmail = etEmail.text.toString().trim()
                val newTelp = etTelepon.text.toString().trim()
                val newCabang = spCabang.selectedItem.toString()

                if (newNama.isEmpty() || newEmail.isEmpty() || newTelp.isEmpty() || newCabang == "Pilih Cabang") {
                    Toast.makeText(context, "Semua input harus diisi dengan benar", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedPegawai = Pegawai(pegawai.id, newNama, newEmail, newTelp, newCabang)
                FirebaseDatabase.getInstance().getReference("pegawai").child(pegawai.id).setValue(updatedPegawai)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Data pegawai berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Gagal memperbarui data pegawai", Toast.LENGTH_SHORT).show()
                    }
            }

            dialogBuilder.setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }

            dialogBuilder.create().show()
        }
    }
}
