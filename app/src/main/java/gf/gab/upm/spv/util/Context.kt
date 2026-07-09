package gf.gab.upm.spv.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Get an [Activity] from a context.
 */
tailrec fun Context.getActivity(): Activity? = when(this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}
