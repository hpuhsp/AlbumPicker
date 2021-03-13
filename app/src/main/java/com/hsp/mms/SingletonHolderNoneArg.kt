package com.hsp.mms

/**
 * Used to allow Singleton with arguments in Kotlin while keeping the code efficient and safe.
 */
open class SingletonHolderNoneArg<out T>(private val creator: () -> T) {

    @Volatile
    private var instance: T? = null

    fun getInstance(): T =
        instance ?: synchronized(this) {
            instance ?: creator().apply {
                instance = this
            }
        }
}
