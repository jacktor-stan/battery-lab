package com.jacktor.batterylab.fragments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.jacktor.batterylab.BuildConfig
import com.jacktor.batterylab.MainApp
import com.jacktor.batterylab.MainApp.Companion.isInstalledGooglePlay
import com.jacktor.batterylab.R
import com.jacktor.batterylab.adapters.TeamAdapter
import com.jacktor.batterylab.interfaces.CheckUpdateInterface
import com.jacktor.batterylab.interfaces.PremiumInterface
import com.jacktor.batterylab.interfaces.RecyclerTeamInterface
import com.jacktor.batterylab.utilities.Constants.GITHUB_LINK
import com.jacktor.batterylab.utilities.Constants.GITHUB_LINK_BATTERY_CAPCITY
import com.jacktor.batterylab.utilities.Prefs
import com.jacktor.batterylab.views.TeamModel
import org.json.JSONObject
import java.io.IOException

class AboutFragment : PreferenceFragmentCompat(), PremiumInterface, RecyclerTeamInterface,
    CheckUpdateInterface {

    var pref: Prefs? = null
    private var checkUpdate: Preference? = null
    private var developer: Preference? = null
    private var version: Preference? = null
    private var build: Preference? = null
    private var buildDate: Preference? = null
    private var github: Preference? = null
    private var githubBC: Preference? = null
    private var betaTester: Preference? = null
    private var team: Preference? = null

    private lateinit var teamModelArrayList: ArrayList<TeamModel>


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        pref = Prefs(requireContext())

        addPreferencesFromResource(R.xml.about_settings)

        developer = findPreference("developer")

        version = findPreference("version")

        build = findPreference("build")

        buildDate = findPreference("build_date")

        checkUpdate = findPreference("check_update")

        github = findPreference("github")
        githubBC = findPreference("github_battery_capacity")

        betaTester = findPreference("become_a_beta_tester")

        checkUpdate?.apply {
            isVisible = isInstalledGooglePlay && MainApp.isGooglePlay(requireContext())
            setOnPreferenceClickListener {
                checkUpdateFromGooglePlay()
                true
            }
        }

        team = findPreference("team")

        betaTester?.isVisible = isInstalledGooglePlay

        version?.summary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requireContext().packageManager?.getPackageInfo(
                requireContext().packageName,
                PackageManager.PackageInfoFlags.of(0)
            )?.versionName
        else {
            requireContext().packageManager?.getPackageInfo(
                requireContext().packageName,
                0
            )?.versionName
        }

        build?.summary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requireContext().packageManager?.getPackageInfo(
                requireContext().packageName,
                PackageManager.PackageInfoFlags.of(0)
            )?.let {
                PackageInfoCompat.getLongVersionCode(it).toString()
            }
        else {
            requireContext().packageManager?.getPackageInfo(
                requireContext().packageName,
                0
            )?.let { PackageInfoCompat.getLongVersionCode(it).toString() }
        }

        buildDate?.summary = BuildConfig.BUILD_DATE

        developer?.setOnPreferenceClickListener {

            try {

                if (isInstalledGooglePlay)
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://search?q=pub:${developer?.summary}")
                        )
                    )
                else startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://play.google.com/store/apps/developer?id=${
                                developer
                                    ?.summary
                            }"
                        )
                    )
                )
            } catch (e: ActivityNotFoundException) {

                Toast.makeText(
                    requireContext(), e.message ?: e.toString(),
                    Toast.LENGTH_LONG
                ).show()
            }

            true
        }

        github?.setOnPreferenceClickListener {

            try {

                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_LINK)))
            } catch (e: ActivityNotFoundException) {

                Toast.makeText(
                    requireContext(), e.message ?: e.toString(),
                    Toast.LENGTH_LONG
                ).show()
            }

            true
        }

        githubBC?.setOnPreferenceClickListener {

            try {

                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_LINK_BATTERY_CAPCITY)))
            } catch (e: ActivityNotFoundException) {

                Toast.makeText(
                    requireContext(), e.message ?: e.toString(),
                    Toast.LENGTH_LONG
                ).show()
            }

            true
        }

        betaTester?.setOnPreferenceClickListener {

            try {

                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://play.google.com/apps/testing/${
                                requireContext()
                                    .packageName
                            }"
                        )
                    )
                )
            } catch (e: ActivityNotFoundException) {

                Toast.makeText(
                    requireContext(), e.message ?: e.toString(),
                    Toast.LENGTH_LONG
                ).show()
            }

            true
        }



        team?.setOnPreferenceClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.TeamDialog)
            val inflater = layoutInflater
            val customView = inflater.inflate(R.layout.team_dialog, null)
            val recyclerView = customView.findViewById<RecyclerView>(R.id.team_recycler)
            val progressBar = customView.findViewById<LinearProgressIndicator>(R.id.progressBar)


            //Offline atau gagal
            val obj = JSONObject(fetchingLocalJson(requireContext(), "team.json")!!)
            teamModelArrayList = ArrayList()
            val dataArray = obj.getJSONArray("data")

            for (i in 0 until dataArray.length()) {

                val teamModel = TeamModel()
                val dataObj = dataArray.getJSONObject(i)

                teamModel.setNames(dataObj.getString("name"))
                teamModel.setUsernames(dataObj.getString("username"))
                teamModel.setStatus(dataObj.getString("projectStatus"))
                teamModel.setimgURLs(dataObj.getString("imgURL"))

                teamModelArrayList.add(teamModel)

            }

            if (isAdded) {
                recyclerView!!.adapter =
                    TeamAdapter(requireContext(), teamModelArrayList, this)
                recyclerView.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            }

            progressBar?.visibility = View.GONE

            dialog.setTitle(getString(R.string.team))
            dialog.setView(customView)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok)) { dialogBtn, _ ->
                    // Respond to neutral button press
                    dialogBtn.cancel()
                }
                .show()
            true
        }
    }

    @Suppress("SameParameterValue")
    private fun fetchingLocalJson(context: Context, filename: String): String? {
        val json: String
        try {
            val inputStream = context.assets.open(filename)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.use { it.read(buffer) }
            json = String(buffer)
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return null
        }
        return json
    }

    //Visit Profile
    override fun onItemClick(data: TeamModel) {
        val openURL = Intent(Intent.ACTION_VIEW)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.visit_this_profile))
            .setMessage(data.name + " (" + data.username + ")")
            .setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                // Respond to neutral button press
                dialog.cancel()
            }
            //.setNegativeButton(getString(R.string.decline)) { dialog, which ->
            // Respond to negative button press
            //}
            .setPositiveButton(getString(R.string.yes_continue)) { _, _ ->
                openURL.data = Uri.parse("https://jacktor.com/members/" + data.username + "/")
                startActivity(openURL)
            }
            .show()

    }


    override fun onResume() {

        super.onResume()

        betaTester?.isVisible = isInstalledGooglePlay
    }
}