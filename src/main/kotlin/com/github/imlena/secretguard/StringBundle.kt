package com.github.imlena.secretguard

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val STRING_BUNDLE = "messages.StringBundle"

object StringBundle : DynamicBundle(STRING_BUNDLE) {

    @JvmStatic
    fun string(@PropertyKey(resourceBundle = STRING_BUNDLE) key: String, vararg params: Any) =
        getMessage(key, *params)

    @Suppress("unused")
    @JvmStatic
    fun stringPointer(@PropertyKey(resourceBundle = STRING_BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)
}