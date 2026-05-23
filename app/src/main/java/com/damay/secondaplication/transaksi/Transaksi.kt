package com.damay.secondaplication.transaksi

data class Transaksi(
    val id: String = "",
    val namaProduk: String = "",
    val jumlah: Int = 0,
    val hargaTotal: Double = 0.0,
    val tanggal: String = ""
)
