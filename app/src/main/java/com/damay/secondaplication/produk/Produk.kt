package com.damay.secondaplication.produk

data class Produk(
    val id: String = "",
    val namaProduk: String = "",
    val kategori: String = "",
    val harga: Double = 0.0,
    val stok: Int = 0
)
