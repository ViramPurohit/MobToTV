package shaadi.com.db

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(
    entities = [
        ShaadiUsers :: class],
    version = 1,
    exportSchema = false
)
abstract class ShaadiDb : RoomDatabase() {

    abstract fun shaadiUserDao() : ShaadiUserDao
}