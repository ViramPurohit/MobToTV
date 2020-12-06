package shaadi.com.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import com.google.gson.annotations.SerializedName

@Entity(
    indices = [
        Index("gender"),
        Index("name_title"),
        Index("location_city"),
        Index("login_uuid"),
        Index("dob_date"),
        Index("registered_date"),
        Index("id_name"),
        Index("picture_large")
//        ,
//        Index("street__number"),
//        Index("coordinates_latitude"),
//        Index("timezone_offset")
    ],
    primaryKeys = ["login_uuid"]
)
data class ShaadiUsers (
    @field:SerializedName("gender") val gender : String?,
    @field:Embedded(prefix = "name_")
    @field:SerializedName("name")
    val name : Name,


    @field:Embedded(prefix = "location_")
    @field:SerializedName("location")
    val location : Location,

    @field:SerializedName("email") val email : String?,
    @field:Embedded(prefix = "login_")
    @field:SerializedName("login")
    val login : Login,

    @field:Embedded(prefix = "dob_")
    @field:SerializedName("dob")
    val dob : Dob,

    @field:Embedded(prefix = "registered_")
    @field:SerializedName("registered")
    val registered : Registered,

    @field:SerializedName("phone") val phone : String?,
    @field:SerializedName("cell") val cell : String?,

    @field:Embedded(prefix = "id_")
    @field:SerializedName("id")
    val id : Id,

    @field:Embedded(prefix = "picture_")
    @field:SerializedName("picture")
    val picture : Picture,

    @field:SerializedName("nat") val nat : String?,
    @field:SerializedName("status") var status : String?
)

data class Name (

    @field:SerializedName("title") val title : String?,
    @field:SerializedName("first") val first : String?,
    @field:SerializedName("last") val last : String?
)

data class Login (

    @field:SerializedName("uuid") val uuid : String,
    @field:SerializedName("username") val username : String?,
    @field:SerializedName("password") val password : String?,
    @field:SerializedName("salt") val salt : String?,
    @field:SerializedName("md5") val md5 : String?,
    @field:SerializedName("sha1") val sha1 : String?,
    @field:SerializedName("sha256") val sha256 : String?
)
data class Dob (

    @field:SerializedName("date") val date : String?,
    @field:SerializedName("age") val age : Int
)
data class Id (

    @field:SerializedName("name") val name : String?,
    @field:SerializedName("value") val value : String?
)
data class Registered (

    @field:SerializedName("date") val date : String?,
    @field:SerializedName("age") val age : Int
)

data class Location (

    @field:Embedded(prefix = "street_")
    @field:SerializedName("street") val street : Street,

    @field:SerializedName("city") val city : String?,
    @field:SerializedName("state") val state : String?,
    @field:SerializedName("country") val country : String?,
    @field:SerializedName("postcode") val postcode : String?,


    @field:Embedded(prefix = "coordinates_")
    @field:SerializedName("coordinates")
    val coordinates : Coordinates,

    @field:Embedded(prefix = "timezone_")
    @field:SerializedName("timezone")
    val timezone : Timezone

)
data class Street (

    @field:SerializedName("number") val number : Int,
    @field:SerializedName("name") val name : String?
)
data class Coordinates (

    @field:SerializedName("latitude") val latitude : Double,
    @field:SerializedName("longitude") val longitude : Double
)
data class Timezone (

    @field:SerializedName("offset") val offset : String?,
    @field:SerializedName("description") val description : String?
)
data class Picture (

    @field:SerializedName("large") val large : String?,
    @field:SerializedName("medium") val medium : String?,
    @field:SerializedName("thumbnail") val thumbnail : String?
)
