package com.damay.secondaplication.produk

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Produk(
    val id: String? = null,
    val namaProduk: String? = "",
    val kategori: String? = "",
    val harga: Double = 0.0,
    val stok: Int = 0
)