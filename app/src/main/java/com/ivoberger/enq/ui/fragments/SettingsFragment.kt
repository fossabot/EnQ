package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.ivoberger.enq.persistence.Configuration
import com.ivoberger.enq.utils.toastShort

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)

        screen.addPreference(Preference(context).apply {
            title = "Clear saved users"
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                Configuration(context).savedUsers = null
                context.toastShort("Cleared saved users")
                return@OnPreferenceClickListener true
            }
        })

        preferenceScreen = screen
    }
}
