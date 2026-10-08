package com.ledinhthi.ontaptld

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Bản debug: provider in ra Logcat một debug token (tag `DebugAppCheckProvider`) ở lần chạy
 * đầu. Dán token đó vào Firebase Console → App Check → Apps → Manage debug tokens thì các
 * request từ máy dev mới qua được Enforce.
 */
object AppCheckInstaller {
    fun install() = Firebase.appCheck
        .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
