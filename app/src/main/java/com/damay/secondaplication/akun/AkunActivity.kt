package com.damay.secondaplication.akun

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.damay.secondaplication.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*

class AkunActivity : AppCompatActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnSimpan: Button
    private lateinit var rgTheme: RadioGroup
    private lateinit var rbThemeLight: RadioButton
    private lateinit var rbThemeDark: RadioButton
    private lateinit var rbThemeSystem: RadioButton

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var databaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_akun)

        initViews()
        setupFirebase()
        loadThemePrefs()
        loadProfileData()

        btnSimpan.setOnClickListener {
            simpanProfile()
        }

        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            applyThemeChange(checkedId)
        }
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnSimpan = findViewById(R.id.btnSimpanAkun)
        rgTheme = findViewById(R.id.rgTheme)
        rbThemeLight = findViewById(R.id.rbThemeLight)
        rbThemeDark = findViewById(R.id.rbThemeDark)
        rbThemeSystem = findViewById(R.id.rbThemeSystem)

        sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE)
        etUsername.isEnabled = false
    }

    private fun setupFirebase() {
        databaseRef = FirebaseDatabase.getInstance().getReference("akun")
    }

    private fun loadProfileData() {
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").value?.toString() ?: "damay.admin"
                val password = snapshot.child("password").value?.toString() ?: "password"
                etUsername.setText(username)
                etPassword.setText(password)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun simpanProfile() {
        val password = etPassword.text.toString().trim()
        if (password.isEmpty()) {
            etPassword.error = "Password tidak boleh kosong"
            return
        }

        databaseRef.child("password").setValue(password)
            .addOnSuccessListener {
                Toast.makeText(this, "Profil berhasil disimpan", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menyimpan profil", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadThemePrefs() {
        val currentTheme = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> rbThemeLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> rbThemeDark.isChecked = true
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> rbThemeSystem.isChecked = true
        }
    }

    private fun applyThemeChange(checkedId: Int) {
        val selectedMode = when (checkedId) {
            R.id.rbThemeLight -> AppCompatDelegate.MODE_NIGHT_NO
            R.id.rbThemeDark -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        // Save preference
        sharedPreferences.edit().putInt("theme_mode", selectedMode).apply()

        // Set night mode
        AppCompatDelegate.setDefaultNightMode(selectedMode)
    }
}
