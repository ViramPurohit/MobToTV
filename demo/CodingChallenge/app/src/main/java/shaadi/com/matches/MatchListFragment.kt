package shaadi.com.matches

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_match_list.*
import shaadi.com.R
import shaadi.com.adapter.ShaadiUserListAdapter
import shaadi.com.api.UserListState
import shaadi.com.base.BaseBindingFragment
import shaadi.com.base.gone
import shaadi.com.base.showLong
import shaadi.com.base.visible
import shaadi.com.databinding.FragmentMatchListBinding
import shaadi.com.db.ShaadiUsers


class MatchListFragment : BaseBindingFragment<FragmentMatchListBinding,
        MatchListViewModel>(MatchListViewModel::class.java) {

    override fun assignBindingAttributes(binding: FragmentMatchListBinding) {
        binding.matchListViewModel = viewModel
    }

    override fun layoutId() = R.layout.fragment_match_list

    private var userListAdapter: ShaadiUserListAdapter? = null
    private lateinit var mContext : Context
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }
    companion object {

        @JvmStatic
        fun newInstance() = MatchListFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
    }

    fun init(){

        initState()



        userlist_recycleview.apply {

            // set a LinearLayoutManager to handle Android
            // RecyclerView behavior
            layoutManager =
                LinearLayoutManager(context)

            val dividerItemDecoration = DividerItemDecoration(
                userlist_recycleview.getContext(),
                (layoutManager as LinearLayoutManager).getOrientation()
            )
            userlist_recycleview.addItemDecoration(dividerItemDecoration)
            setHasFixedSize(true)
        }

        /*
        * get User count
        * */
        viewModel.getUserCount()

    }

    private fun initState() {
        viewModel.getState()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { state ->
                when (state) {
                    is UserListState.Error -> {
                        userProgress.let {
                            userProgress.gone()
                        }
                    }
                    is UserListState.UserListSuccess -> {
                        userProgress.let {
                            userProgress.gone()
                        }
                        if(state.userListResponse.results.isNotEmpty()){
                            setUserDetails(state.userListResponse.results)

                        }else{
                            showLong(userlist_recycleview,
                                getString(R.string.no_users))
                        }

                    }
                    is UserListState.UserDBListSuccess -> {
                        userProgress.let {
                            userProgress.gone()
                        }
                        if(state.userListResponse.isNotEmpty()){
                            setUserDetails(state.userListResponse)

                        }else{
                            showLong(userlist_recycleview,
                                getString(R.string.no_users))
                        }

                    }
                    is UserListState.UserListCount -> {
                        userProgress.let {
                            userProgress.gone()
                        }

                        if(state.userCount > 0){
                            viewModel.getAllDBUsers()
                        }else{
                            viewModel.getUserList(mContext,10)
                        }
                    }
                    is UserListState.UserListRequested -> {
                        userProgress.visible()

                    }
                    is UserListState.UserMessage -> {
                        userProgress.let {
                            userProgress.gone()
                        }
                        showLong(userProgress,state.message)

                    }
                }
            }.addTo(disposable)
    }



    private fun setUserDetails(results: List<ShaadiUsers>) {
        userListAdapter = context?.let {
            ShaadiUserListAdapter(it,
                results as MutableList<ShaadiUsers>
            ) { shaadiUsers: ShaadiUsers, status: String, pos: Int ->

                shaadiUsers.status = status
                updateUserDetails(shaadiUsers,pos)


            }
        }
        userlist_recycleview.adapter = userListAdapter


    }

    fun updateUserDetails(
        shaadiUsers: ShaadiUsers,
        pos: Int
    ){
        viewModel.updateUser(shaadiUsers)
        userListAdapter?.updateData(shaadiUsers,pos)

    }
}
