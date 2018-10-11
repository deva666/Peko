package com.markodevcic.peko

import android.content.SharedPreferences

internal interface RationaleChecker {

    fun checkIfRationaleShownAlready(permission: String): Boolean

    fun setRationaleShownFor(permissions: Collection<String>)

    companion object {
        fun default(sharedPreferences: SharedPreferences): RationaleChecker = RationaleCheckerImpl(sharedPreferences)
    }
}

internal class RationaleCheckerImpl(private val sharedPreferences: SharedPreferences) : RationaleChecker {
    override fun checkIfRationaleShownAlready(permission: String): Boolean {
        val rationaleShowedSet = sharedPreferences.getStringSet(RATIONALE_SHOWED_SET_KEY, mutableSetOf())
        return rationaleShowedSet.contains(permission)
    }

    override fun setRationaleShownFor(permissions: Collection<String>) {
        val rationaleShowedSet = sharedPreferences.getStringSet(RATIONALE_SHOWED_SET_KEY, mutableSetOf())
        rationaleShowedSet.addAll(permissions)
        sharedPreferences.edit()
                .remove(RATIONALE_SHOWED_SET_KEY)
                .apply()
        sharedPreferences.edit()
                .putStringSet(RATIONALE_SHOWED_SET_KEY, rationaleShowedSet)
                .apply()
    }
}