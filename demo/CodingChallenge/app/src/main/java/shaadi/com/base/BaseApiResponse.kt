package com.android.cybay.base

import com.google.gson.annotations.SerializedName

open class BaseApiResponse : BaseModel() {

    @SerializedName("error")
    var error: String? = null

    @SerializedName("status")
    var status: String? = null

    @SerializedName("error_description")
    var errorDescription: String? = null

    fun ok(): Boolean {
        return error.isNullOrEmpty() || status == STATUS_OK
    }

    companion object {
        const val STATUS_OK = "ok"
    }
}
