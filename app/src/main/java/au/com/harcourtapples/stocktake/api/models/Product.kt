package au.com.harcourtapples.stocktake.api.models

import com.google.gson.annotations.SerializedName

data class Product(
    val barcode: String,
    val description: String,
    @SerializedName("sell_price") val sellPrice: Double,
    @SerializedName("cost_price") val costPrice: Double,
    val unit: String,
    val brand: String?,
    @SerializedName("dept_name") val deptName: String?,
    @SerializedName("soh_qty") val sohQty: Double
)
