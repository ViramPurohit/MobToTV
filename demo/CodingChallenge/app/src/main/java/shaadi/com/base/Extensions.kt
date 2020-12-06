package shaadi.com.base

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar


fun Context.makeShortToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.makeLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}




fun showLong(container: View, message: String) {
    return Snackbar.make(container, message, Snackbar.LENGTH_LONG)
        .show()
}

fun showLong(container: View, @StringRes resource: Int) {
    return Snackbar
        .make(container, container.context.resources.getString(resource), Snackbar.LENGTH_LONG)
        .show()
}