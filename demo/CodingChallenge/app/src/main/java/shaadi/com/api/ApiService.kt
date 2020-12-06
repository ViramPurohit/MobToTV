package shaadi.com.api

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.reactivestreams.Publisher
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import shaadi.com.model.UserListResponse

interface ApiService{
    @GET("api/")
    fun productList(
        @Query("results")  limit :Int
    ): Call<UserListResponse>

}
