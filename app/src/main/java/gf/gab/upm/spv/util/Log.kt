package gf.gab.upm.spv.util

import android.util.Log

fun Any.logVerbose(message: String) = Log.v(getClassName(), message)
fun Any.logDebug(message: String) = Log.d(getClassName(), message)
fun Any.logInfo(message: String) = Log.i(getClassName(), message)
fun Any.logWarn(message: String) = Log.w(getClassName(), message)
fun Any.logError(message: String) = Log.e(getClassName(), message)

private fun Any.getClassName(): String = this::class.simpleName ?: this::class.java.name
