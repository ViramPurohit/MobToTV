package shaadi.com.base

import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

open class BaseViewModel<T> : ViewModel(), Observable {

    @Transient
    private var transientCallbacks: PropertyChangeRegistry? = null

    var disposable = CompositeDisposable()

    protected val statePublisher = PublishSubject.create<T>()

    /**
     * Get way to communicate between view model to view/fragment
     */
    fun getState() = statePublisher

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
        synchronized(this) {
            if (transientCallbacks == null) {
                transientCallbacks = PropertyChangeRegistry()
            }
        }
        transientCallbacks?.add(callback)
    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
        synchronized(this) {
            if (transientCallbacks == null) {
                return
            }
        }
        transientCallbacks?.remove(callback)
    }

    fun publish(state: T) {
        statePublisher.onNext(state)
    }

    /**
     * Notifies listeners that all properties of this instance have changed.
     */
    fun notifyChange() {
        synchronized(this) {
            if (transientCallbacks == null) {
                return
            }
        }
        transientCallbacks?.notifyCallbacks(this, 0, null)
    }

    /**
     * Notifies listeners that a specific property has changed. The getter for the property
     * that changes should be marked with [Bindable] to generate a field in
     * `BR` to be used as `fieldId`.

     * @param fieldId The generated BR id for the Bindable field.
     */
    fun notifyPropertyChanged(fieldId: Int) {
        synchronized(this) {
            if (transientCallbacks == null) {
                return
            }
        }
        transientCallbacks?.notifyCallbacks(this, fieldId, null)
    }

    override fun onCleared() {
        disposable.clear()
        super.onCleared()
    }
}
