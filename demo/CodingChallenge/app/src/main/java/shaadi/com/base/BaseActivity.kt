package shaadi.com.base

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.disposables.CompositeDisposable
import shaadi.com.R
import javax.inject.Inject

abstract class BaseActivity :  AppCompatActivity(),HasAndroidInjector {


    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    lateinit var disposable: CompositeDisposable

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory


    abstract fun layoutId(): Int

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(layoutId())
        disposable = CompositeDisposable()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount >= 1) {
            // We need to check last fragment if it handle back press i.e PrepareOrderFragment
            supportFragmentManager.fragments.reversed().forEach { fragment ->
                if (fragment is BaseFragment) {
                    if (!fragment.onBackPressed()) {
                        if (supportFragmentManager.backStackEntryCount == 1) finish()
                        else super.onBackPressed()
                    } else super.onBackPressed()
                    return
                } else {
                    super.onBackPressed()
                }
            }
        } else
            super.onBackPressed()


    }

    fun addFragments(
        supportFragmentManager: FragmentManager,
        fragment: Fragment
    ) {

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.addToBackStack("")
        fragmentTransaction.add(R.id.main_container, fragment, fragment.javaClass.name)
        fragmentTransaction.commitAllowingStateLoss()
    }


    override fun androidInjector(): AndroidInjector<Any> {
        return dispatchingAndroidInjector
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }


}
