package shaadi.com.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import io.reactivex.disposables.CompositeDisposable

abstract class BaseBindingFragment<B : ViewDataBinding, T : ViewModel>(
    private val cls: Class<T>
) : BaseFragment() {

    private var _binding: AutoClearedValue<B>? = null
    lateinit var viewModel: T

    var disposable = CompositeDisposable()

    val binding: AutoClearedValue<B>
        get() = _binding!!

    final override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflateView(inflater, container, savedInstanceState)
    }


    open fun inflateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val databinding: B = DataBindingUtil.inflate(inflater, layoutId(), container, false)
        _binding = AutoClearedValue(this, databinding)
        initBinding(databinding)
        return databinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        disposable = CompositeDisposable()
    }

    override fun onDestroyView() {
        disposable.clear()
        super.onDestroyView()
    }

    private fun initBinding(binding: B) {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(cls)
        assignBindingAttributes(binding)
    }

    abstract fun assignBindingAttributes(binding: B)

}
