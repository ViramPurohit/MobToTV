package shaadi.com.api

import io.reactivex.Observable
import shaadi.com.base.BaseRepository
import shaadi.com.db.ShaadiUserDao
import shaadi.com.db.ShaadiUsers
import shaadi.com.model.UserListResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShaadiRepository @Inject constructor(
    private val shaadiUserDao: ShaadiUserDao,
    private val apiService: ApiService
) : BaseRepository() {

    /*
 * Fetch users from API
 * */
    fun fetchUserDetails(size: Int): Observable<UserListResponse> {
        return Observable.create { emitter ->
            apiService.productList(size)
                .enqueue(getBaseCallback<UserListResponse,
                        UserListResponse>(emitter) { response ->
                    emitter.onNext(response)
                })
        }
    }

    /*
    * Get Shaadi user count
    * */
    fun getShaadiUserCount(): Observable<Int> {
        return Observable.create {
                emitter -> emitter.onNext(shaadiUserDao.getUserCount())
        }
    }



    fun getAll(): Observable<List<ShaadiUsers>> {
        return Observable.create {
                emitter -> emitter.onNext(shaadiUserDao.getAll())
        }
    }

    /*
    * Add users
    * */
    fun insertUser(results: List<ShaadiUsers>) {
        shaadiUserDao.insertUsers(results)
    }




    /*Update users*/
    fun updateUsers(shaadiUsers: ShaadiUsers) {
        return shaadiUserDao.updateUsers(shaadiUsers)
    }

}