package shaadi.com.matches

import android.content.Context
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Action
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import shaadi.com.api.ShaadiRepository
import shaadi.com.api.UserListState
import shaadi.com.base.BaseViewModel
import shaadi.com.db.ShaadiUsers
import shaadi.com.utils.Common
import javax.inject.Inject


class MatchListViewModel
@Inject constructor(
    private val registerRepository: ShaadiRepository
) : BaseViewModel<UserListState>() {


    fun getUserCount()  {
        registerRepository.getShaadiUserCount()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({
                publish(UserListState.UserListCount(it))
            },
                {
                    it.printStackTrace()
                    publish(UserListState.Error(it))
                }).addTo(disposable)
    }

    fun getUserList(mContext: Context, size : Int) {

        if(Common.isNetworkAvailable(mContext)){
                            try {
                                registerRepository
                                    .fetchUserDetails(size)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .doOnSubscribe { publish(UserListState.UserListRequested) }.subscribeOn(Schedulers.io())
                                    .subscribe(
                                        {

                                            addAllUser(it.results)
                                            publish(UserListState.UserListSuccess(it))

                                        },
                                        {
                                            it.printStackTrace()
                                            publish(UserListState.Error(it))
                                        }
                                    ).addTo(disposable)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }else{
                            publish(UserListState.UserMessage("No Internet"))
                        }
        

    }

    fun getAllDBUsers(){
        registerRepository.getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({
                publish(UserListState.UserDBListSuccess(it))
            },
                {
                    it.printStackTrace()
                    publish(UserListState.Error(it))
                }).addTo(disposable)
    }


    fun addAllUser(results: List<ShaadiUsers>) {

        Completable.fromAction(object : Action {
            override fun run() {
                registerRepository.insertUser(results)
            }
        })
            .subscribeOn(Schedulers.io())
            .subscribe(object : CompletableObserver {
                override fun onSubscribe(d: Disposable) {}

                override fun onComplete() {
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                }
            })
    }


    fun updateUser(shaadiUsers: ShaadiUsers) {
        registerRepository.updateUsers(shaadiUsers)

    }


}

