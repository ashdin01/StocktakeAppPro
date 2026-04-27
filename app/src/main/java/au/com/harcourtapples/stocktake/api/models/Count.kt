package au.com.harcourtapples.stocktake.api.models

import com.google.gson.annotations.SerializedName

data class Count(
    val id: Int,
    @SerializedName("session_id") val sessionId: Int,
    val barcode: String,
    @SerializedName("counted_qty") val countedQty: Double,
    val description: String,
    @SerializedName("sell_price") val sellPrice: Double,
    @SerializedName("dept_name") val deptName: String?,
    @SerializedName("scanned_at") val scannedAt: String
)

data class AddCountRequest(
    val barcode: String,
    val qty: Double
)
