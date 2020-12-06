package shaadi.com.model

import com.google.gson.annotations.SerializedName
import shaadi.com.db.ShaadiUsers

data class UserListResponse(
    @SerializedName("results") val results : List<ShaadiUsers>,
    @SerializedName("info") val info : Info
)
data class Info (

    @SerializedName("seed") val seed : String,
    @SerializedName("results") val results : Int,
    @SerializedName("page") val page : Int,
    @SerializedName("version") val version : Double
)