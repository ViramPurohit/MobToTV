package shaadi.com.db

import androidx.room.*
import io.reactivex.Flowable
import io.reactivex.Single
import org.jetbrains.annotations.NotNull


@Dao
interface ShaadiUserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUsers(shaadiUsers: List<ShaadiUsers>)

    @Query("SELECT COUNT(login_uuid) FROM shaadiUsers")
    fun getUserCount(): Int


    @Update
    fun updateUsers(shaadiUsers : ShaadiUsers)


    @Query("SELECT * FROM shaadiUsers")
    fun getAll(): List<ShaadiUsers>
}