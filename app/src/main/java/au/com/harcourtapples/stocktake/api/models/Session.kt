package au.com.harcourtapples.stocktake.api.models

import com.google.gson.annotations.SerializedName

data class Session(
    val id: Int,
    val label: String,
    @SerializedName("dept_name") val deptName: String?,
    @SerializedName("group_name") val groupName: String?,
    val status: String,
    @SerializedName("started_at") val startedAt: String,
    @SerializedName("closed_at") val closedAt: String?,
    @SerializedName("line_count") val lineCount: Int,
    val notes: String?
)

data class CreateSessionRequest(
    val label: String,
    @SerializedName("department_id") val departmentId: Int?,
    @SerializedName("group_id") val groupId: Int? = null,
    val notes: String = "",
    @SerializedName("created_by") val createdBy: String = "Android"
)
