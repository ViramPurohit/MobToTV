package shaadi.com.di.modules

import dagger.Module
import dagger.android.ContributesAndroidInjector
import shaadi.com.matches.MatchListActivity

@Module(includes = [FragmentBuilderModule::class])
abstract class ActivityBuilderModule {
    /*
    * Need to add All Classes those inject this  Modules
    * */
    @ContributesAndroidInjector
    abstract fun bindMatchListActivity() : MatchListActivity
}