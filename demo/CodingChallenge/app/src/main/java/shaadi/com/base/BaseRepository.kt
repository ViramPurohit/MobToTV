package shaadi.com.base

import com.android.cybay.base.BaseApiResponse
import io.reactivex.ObservableEmitter
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

open class BaseRepository() {


    open fun <T, E> getBaseCallback(
        emitter: ObservableEmitter<T>,
        responseListener: ((response: E) -> (Unit))
    ): Callback<E> {
        return object : Callback<E> {
            override fun onFailure(call: Call<E>, t: Throwable) {
                emitter.onError(t)
            }

            override fun onResponse(call: Call<E>, response: Response<E>) {
                try {
                    if (!response.isSuccessful) {
                        response.errorBody()?.string()?.let { string ->
                            val errorMsg : String = if(JSONObject(string).has("errorMsg")){
                                JSONObject(string).optString("errorMsg")
                            }else{
                                JSONObject(string).optString("message")
                            }

                            // Make sure to avoid error delivery in case of observable disposed
                            if (!emitter.isDisposed) {
                                emitter.onError(Exception(errorMsg))
                            }
                        }
                    } else {
                        // Make sure to avoid error delivery in case of observable disposed
                        if (!emitter.isDisposed) {
                            response.body()?.let {
                                if (it is BaseApiResponse && !it.ok()) {
                                    emitter.onError(Exception("Error! ${it.errorDescription}"))
                                } else responseListener.invoke(it)
                            } ?: run {
                                emitter.onError(Exception("Empty body from API response."))
                            }
                        }

                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

}
