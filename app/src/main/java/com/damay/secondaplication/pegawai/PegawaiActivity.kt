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

    private lateinit var spFilterCabang: Spinner

    private val pegawaiList = ArrayList<Pegawai>()
    private val filteredPegawaiList = ArrayList<Pegawai>()
    val cabangList = ArrayList<String>()
    private lateinit var listAdapter: PegawaiAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pegawai)

        listView = findViewById(R.id.listViewPegawai)
        btnTambah = findViewById(R.id.btnTambahPegawai)
        spFilterCabang = findViewById(R.id.spFilterCabang)

        databaseRef = FirebaseDatabase.getInstance().getReference("pegawai")
        cabangRef = FirebaseDatabase.getInstance().getReference("cabang")

        listAdapter = PegawaiAdapter(this, filteredPegawaiList)
        listView.adapter = listAdapter

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cabangList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spFilterCabang.adapter = spinnerAdapter

        spFilterCabang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterPegawai()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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
                filterPegawai()
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
                cabangList.add("Semua Cabang")
                for (child in snapshot.children) {
                    val cab = child.getValue(Cabang::class.java)
                    if (cab != null) {
                        cabangList.add(cab.namaCabang)
                    }
                }
                (spFilterCabang.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun filterPegawai() {
        val selectedCabang = spFilterCabang.selectedItem?.toString() ?: "Semua Cabang"
        filteredPegawaiList.clear()
        
        if (selectedCabang == "Semua Cabang") {
            filteredPegawaiList.addAll(pegawaiList)
        } else {
            for (pegawai in pegawaiList) {
                if (pegawai.cabang == selectedCabang) {
                    filteredPegawaiList.add(pegawai)
                }
            }
        }
        listAdapter.notifyDataSetChanged()
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
            val tvEmail = view.findViewById<TextView>(R.id.tvEmailPegawai)
            val tvTelepon = view.findViewById<TextView>(R.id.tvTeleponPegawai)
            val tvGender = view.findViewById<TextView>(R.id.tvGenderPegawai)
            val tvCabang = view.findViewById<TextView>(R.id.tvCabangPegawai)
            val tvJabatan = view.findViewById<TextView>(R.id.tvJabatanPegawai)

            val btnEdit = view.findViewById<ImageButton>(R.id.btnEditPegawai)
            val btnHapus = view.findViewById<ImageButton>(R.id.btnHapusPegawai)

            tvNama.text = pegawai.nama
            tvEmail.text = "Email: ${pegawai.email}"
            tvTelepon.text = "Telepon: ${pegawai.telepon}"
            tvGender.text = "Gender: ${pegawai.gender}"
            tvCabang.text = "Cabang: ${pegawai.cabang}"
            tvJabatan.text = "Jabatan: ${pegawai.jabatan}"

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

            // Since dialog layout may not have Gender/Jabatan yet, we check for nulls or assume we shouldn't update them here unless we edit dialog layout too.
            // But we must preserve existing gender and jabatan.
            
            // Prefill current data
            etNama.setText(pegawai.nama)
            etEmail.setText(pegawai.email)
            etTelepon.setText(pegawai.telepon)

            // Setup Cabang spinner options
            val activity = context as PegawaiActivity
            val spinnerList = ArrayList<String>()
            spinnerList.add("Pilih Cabang")
            spinnerList.addAll(activity.cabangList.filter { it != "Semua Cabang" })
            val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, spinnerList)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spCabang.adapter = spinnerAdapter

            // Select active branch if match found
            val index = spinnerList.indexOf(pegawai.cabang)
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

                val updatedPegawai = Pegawai(pegawai.id, newNama, newEmail, newTelp, newCabang, pegawai.gender, pegawai.jabatan)
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
