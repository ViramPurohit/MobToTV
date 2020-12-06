package com.android.cybay.base

import androidx.databinding.BaseObservable
import com.google.gson.Gson

open class BaseModel : BaseObservable() {

    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        inline fun <reified T> fromJson(string: String): T {
            return Gson().fromJson<T>(string, T::class.java)
        }
    }
}
