package com.jacktor.batterylab.fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jacktor.batterylab.R
import com.jacktor.batterylab.interfaces.PremiumInterface
import com.jacktor.batterylab.utilities.FileUtils
import com.jacktor.batterylab.utilities.Prefs

class PowerConnectionSettingsFragment : PreferenceFragmentCompat(), PremiumInterface {

    private var enablePowerConnection: SwitchPreferenceCompat? = null
    private var acConnectedSound: Preference? = null
    private var usbConnectedSound: Preference? = null
    private var disconnectedSound: Preference? = null
    private var soundDelay: EditTextPreference? = null
    private var resetSound: Preference? = null
    private var enableVibration: SwitchPreferenceCompat? = null
    private var vibrationDuration: ListPreference? = null
    private var customVibrationDuration: EditTextPreference? = null
    private var vibrationMode: ListPreference? = null
    private var showToast: SwitchPreferenceCompat? = null

    //private var requestCode = 0
    private var fileinfo = 0

    lateinit var pref: Prefs


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            //jika user klik izinkan penyimpanan/audio - pilih file
            showFileChooser()
        } else {
            //jika user klik tolak izin penyimpanan/audio
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionBlockedDialog(R.string.audio_permission_not_granted)
            } else {
                permissionBlockedDialog(R.string.storage_permission_not_granted)
            }
        }
    }


    private val selectFileActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {

            val data: Intent? = result.data
            val filePath = FileUtils.getRealPath(requireContext(), data?.data!!)!!.toUri()

            //Ac Sound Pref
            if (fileinfo == 1) {
                pref.setString("ac_connected_sound", filePath.toString())
            }

            //USB Sound Pref
            if (fileinfo == 2) {
                pref.setString("usb_connected_sound", filePath.toString())

            }

            //Disconnected Sound Pref
            if (fileinfo == 3) {
                pref.setString("disconnected_sound", filePath.toString())
            }

            //Toast.makeText(requireContext(), R.string.changes_saved, Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        pref = Prefs(requireContext())
        addPreferencesFromResource(R.xml.power_connection_settings)

        enablePowerConnection = findPreference("power_connection_service")
        acConnectedSound = findPreference("ac_connected_sound")
        usbConnectedSound = findPreference("usb_connected_sound")
        disconnectedSound = findPreference("disconnected_sound")
        soundDelay = findPreference("sound_delay")
        resetSound = findPreference("reset_sound")
        enableVibration = findPreference("enable_vibration")
        vibrationDuration = findPreference("vibrate_duration")
        customVibrationDuration = findPreference("custom_vibrate_duration")
        vibrationMode = findPreference("vibrate_mode")
        showToast = findPreference("enable_toast")

        //OnPreferenceClickListener
        enablePowerConnection?.setOnPreferenceChangeListener { _, newValue ->

            when (newValue as? Boolean) {

                true -> {
                    disableEnableService(newValue)
                }

                false -> {
                    disableEnableService(newValue)
                }

                null -> {}
            }
            true
        }



        acConnectedSound?.setOnPreferenceClickListener {
            fileinfo = 1
            askPermissionAndBrowseFile()
            true
        }

        usbConnectedSound?.setOnPreferenceClickListener {
            fileinfo = 2
            askPermissionAndBrowseFile()
            true
        }

        disconnectedSound?.setOnPreferenceClickListener {
            fileinfo = 3
            askPermissionAndBrowseFile()
            true
        }

        resetSound?.setOnPreferenceClickListener {
            pref.setString("ac_connected_sound", "")
            pref.setString("usb_connected_sound", "")
            pref.setString("disconnected_sound", "")

            setSummary()

            true
        }

        disableEnableService(pref.getBoolean("power_connection_service", false))
        setSummary()
    }


    private fun askPermissionAndBrowseFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED -> {
                    //jika izin audio tersedia
                    showFileChooser()
                }

                shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_AUDIO) -> {
                    permissionBlockedDialog(R.string.audio_permission_not_granted)
                }

                else -> {
                    // The registered ActivityResultCallback gets the result of this request
                    requestPermissionLauncher.launch(
                        Manifest.permission.READ_MEDIA_AUDIO
                    )
                }
            }
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    //jika izin peyimpanan tersedia
                    showFileChooser()
                }

                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    permissionBlockedDialog(R.string.storage_permission_not_granted)
                }

                else -> {
                    // The registered ActivityResultCallback gets the result of this request
                    requestPermissionLauncher.launch(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }
            }
        }
    }

    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }
        selectFileActivityResult.launch(intent)
    }

    private fun disableEnableService(isEnabled: Boolean) {
        acConnectedSound?.isEnabled = isEnabled
        usbConnectedSound?.isEnabled = isEnabled
        disconnectedSound?.isEnabled = isEnabled
        soundDelay?.isEnabled = isEnabled
        resetSound?.isEnabled = isEnabled
        enableVibration?.isEnabled = isEnabled
        vibrationDuration?.isEnabled = isEnabled
        customVibrationDuration?.isEnabled = isEnabled
        vibrationMode?.isEnabled = isEnabled
        showToast?.isEnabled = isEnabled
    }


    override fun onResume() {
        super.onResume()

        setSummary()
    }


    private fun setSummary() {
        //SET SUMMARY
        // Audio for AC
        val acFile = pref.getString("ac_connected_sound", "")
        if (acFile != "") {
            acConnectedSound?.summary = acFile?.replace("ac_", "")
        } else {
            acConnectedSound?.setSummary(R.string.sound_not_selected)
        }

        // Audio for USB
        val usbFile = pref.getString("usb_connected_sound", "")
        if (usbFile != "") {
            usbConnectedSound?.summary = usbFile?.replace("usb_", "")
        } else {
            usbConnectedSound?.setSummary(R.string.sound_not_selected)
        }

        // Audio for DC
        val dcFile = pref.getString("disconnected_sound", "")
        if (dcFile != "") {
            disconnectedSound?.summary = dcFile?.replace("dc_", "")
        } else {
            disconnectedSound?.setSummary(R.string.sound_not_selected)
        }
    }

    private fun permissionBlockedDialog(msg: Int) {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setMessage(msg)
            setNegativeButton(R.string.open_settings) { _, _ ->

                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }

            setPositiveButton(getString(R.string.dialog_button_close)) { dialog, _ ->
                    dialog.cancel()
                }

            setCancelable(false)

            show()
        }

    }
}