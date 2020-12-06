package shaadi.com.di.modules

import dagger.Module
import dagger.android.ContributesAndroidInjector
import shaadi.com.matches.MatchListFragment

@Module
abstract class FragmentBuilderModule {
    /*
 * Need to add All Classes those inject this  Modules
 * */
    @ContributesAndroidInjector
    abstract fun bindMatchListFragment() : MatchListFragment
}