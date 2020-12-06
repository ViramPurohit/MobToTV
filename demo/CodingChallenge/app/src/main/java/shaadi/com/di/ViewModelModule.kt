package shaadi.com.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import shaadi.com.matches.MatchListViewModel

@Module
abstract class ViewModelModule {

    @Binds
    internal abstract fun bindsViewModelFactory(
        viewModelFactory: CustomViewModelFactory
    ): ViewModelProvider.Factory


    @Binds
    @IntoMap
    @ViewModelKey(MatchListViewModel::class)
    abstract fun bindMatchListViewModel(matchListViewModel: MatchListViewModel): ViewModel


}