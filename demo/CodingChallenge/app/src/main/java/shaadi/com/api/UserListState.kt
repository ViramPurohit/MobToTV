package shaadi.com.api

import shaadi.com.db.ShaadiUsers
import shaadi.com.model.UserListResponse

sealed class UserListState {
    object UserListRequested : UserListState()

    class UserListSuccess(val userListResponse: UserListResponse) : UserListState()
    class UserMessage(val message: String) : UserListState()
    class UserDBListSuccess(val userListResponse: List<ShaadiUsers>) : UserListState()
    class UserListCount(val userCount: Int) : UserListState()

    class Error(val throwable: Throwable) : UserListState()

}