package com.damay.secondaplication.pegawai

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.damay.secondaplication.R
import com.damay.secondaplication.cabang.Cabang
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*

class TambahPegawaiActivity : AppCompatActivity() {

    private lateinit var etNama: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etTelepon: TextInputEditText
    private lateinit var spCabang: Spinner
    private lateinit var spGender: Spinner
    private lateinit var spJabatan: Spinner
    private lateinit var btnSimpan: Button

    private lateinit var databaseRef: DatabaseReference
    private lateinit var cabangRef: DatabaseReference

    private val cabangList = ArrayList<String>()
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_pegawai)

        etNama = findViewById(R.id.etNamaPegawai)
        etEmail = findViewById(R.id.etEmailPegawai)
        etTelepon = findViewById(R.id.etTeleponPegawai)
        spCabang = findViewById(R.id.spCabangPegawai)
        spGender = findViewById(R.id.spGenderPegawai)
        spJabatan = findViewById(R.id.spJabatanPegawai)
        btnSimpan = findViewById(R.id.btnSimpanPegawai)

        databaseRef = FirebaseDatabase.getInstance().getReference("pegawai")
        cabangRef = FirebaseDatabase.getInstance().getReference("cabang")

        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cabangList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCabang.adapter = spinnerAdapter
        
        val genderOptions = arrayOf("Laki-laki", "Perempuan")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spGender.adapter = genderAdapter
        
        val jabatanOptions = arrayOf("Kepala Cabang", "Kasir", "Office Boy")
        val jabatanAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, jabatanOptions)
        jabatanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spJabatan.adapter = jabatanAdapter

        btnSimpan.setOnClickListener {
            simpanPegawai()
        }

        loadCabang()
    }

    private fun loadCabang() {
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
                spinnerAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TambahPegawaiActivity, "Gagal memuat data cabang", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun simpanPegawai() {
        val nama = etNama.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val telp = etTelepon.text.toString().trim()
        val cabang = spCabang.selectedItem?.toString() ?: ""
        val gender = spGender.selectedItem?.toString() ?: ""
        val jabatan = spJabatan.selectedItem?.toString() ?: ""

        if (nama.isEmpty()) {
            etNama.error = "Nama pegawai tidak boleh kosong"
            return
        }

        if (email.isEmpty()) {
            etEmail.error = "Email pegawai tidak boleh kosong"
            return
        }

        if (telp.isEmpty()) {
            etTelepon.error = "Nomor telepon pegawai tidak boleh kosong"
            return
        }

        if (cabang.isEmpty() || cabang == "Pilih Cabang") {
            Toast.makeText(this, "Silakan pilih penempatan cabang pegawai", Toast.LENGTH_SHORT).show()
            return
        }

        val id = databaseRef.push().key
        if (id != null) {
            val pegawai = Pegawai(id, nama, email, telp, cabang, gender, jabatan)
            databaseRef.child(id).setValue(pegawai)
                .addOnSuccessListener {
                    Toast.makeText(this, "Pegawai berhasil disimpan", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal menyimpan pegawai: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
