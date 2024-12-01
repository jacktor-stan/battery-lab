package com.jacktor.batterylab.fragments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textview.MaterialTextView
import com.jacktor.batterylab.BuildConfig
import com.jacktor.batterylab.MainApp
import com.jacktor.batterylab.MainApp.Companion.isInstalledGooglePlay
import com.jacktor.batterylab.R
import com.jacktor.batterylab.adapters.ContributorsAdapter
import com.jacktor.batterylab.interfaces.CheckUpdateInterface
import com.jacktor.batterylab.interfaces.PremiumInterface
import com.jacktor.batterylab.interfaces.RecyclerContributorsInterface
import com.jacktor.batterylab.utilities.Constants.GITHUB_API_CONTRIBUTORS
import com.jacktor.batterylab.utilities.Constants.GITHUB_API_USER
import com.jacktor.batterylab.utilities.Constants.GITHUB_LINK
import com.jacktor.batterylab.utilities.Constants.GITHUB_LINK_BATTERY_CAPCITY
import com.jacktor.batterylab.utilities.preferences.Prefs
import com.jacktor.batterylab.views.ContributorsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AboutFragment : PreferenceFragmentCompat(), PremiumInterface, RecyclerContributorsInterface,
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
    private var contributors: Preference? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var noInternetText: MaterialTextView
    private lateinit var nextButton: MaterialButton
    private lateinit var prevButton: MaterialButton
    private lateinit var progressBar: LinearProgressIndicator

    // Contributors list
    private var contributorsModelArrayList = ArrayList<ContributorsModel>()
    private var currentPage = 0
    private val pageSize = 5
    private var totalPages = 0


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

        contributors = findPreference("contributors")

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

        // Contributors list
        contributors?.setOnPreferenceClickListener {
            showContributorsDialog()
            true
        }
    }


    private fun showContributorsDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.ContributorsDialog)
        val inflater = LayoutInflater.from(requireContext())
        val customView = inflater.inflate(R.layout.contributors_dialog, null)

        recyclerView = customView.findViewById(R.id.contributors_recycler)
        noInternetText = customView.findViewById(R.id.no_internet_text)
        nextButton = customView.findViewById(R.id.next_button)
        prevButton = customView.findViewById(R.id.prev_button)
        progressBar = customView.findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        if (isNetworkAvailable()) {
            fetchContributors()
        } else {
            showNoInternetMessage()
        }

        dialog.setView(customView)
            .setTitle(getString(R.string.contributors))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.close)) { dialogBtn, _ -> dialogBtn.dismiss() }
            .show()

        prevButton.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                displayPage()
            }
        }

        nextButton.setOnClickListener {
            if (currentPage < totalPages - 1) {
                currentPage++
                displayPage()
            }
        }
    }

    private fun fetchContributors() {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contributorsConnection =
                    URL(GITHUB_API_CONTRIBUTORS).openConnection() as HttpURLConnection
                contributorsConnection.connectTimeout = 5000
                contributorsConnection.readTimeout = 5000
                contributorsConnection.requestMethod = "GET"

                if (contributorsConnection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response =
                        contributorsConnection.inputStream.bufferedReader().use { it.readText() }
                    val contributorsArray = JSONArray(response)
                    val contributorsDetails = ArrayList<ContributorsModel>()

                    for (i in 0 until contributorsArray.length()) {
                        val contributor = contributorsArray.getJSONObject(i)
                        val username = contributor.getString("login")
                        val avatarUrl = contributor.optString("avatar_url", "")
                        val contributions = contributor.optInt("contributions", 0)

                        // Fetch nama dari username
                        val name = fetchContributorName(username)

                        contributorsDetails.add(
                            ContributorsModel(
                                name = name ?: username,
                                username = username,
                                avatarUrl = avatarUrl,
                                contributions = contributions
                            )
                        )
                    }

                    // Urutkan berdasarkan kontribusi
                    contributorsDetails.sortByDescending { it.contributions }

                    // Tentukan jumlah total halaman
                    totalPages = (contributorsDetails.size + pageSize - 1) / pageSize

                    // Atur halaman pertama
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        contributorsModelArrayList = contributorsDetails
                        currentPage = 0
                        displayPage()
                    }

                    // Periksa apakah data lebih dari "pageSize" untuk menampilkan tombol Next/Prev
                    if (contributorsModelArrayList.size > pageSize) {
                        nextButton.visibility = View.VISIBLE
                        prevButton.visibility = View.VISIBLE
                    } else {
                        nextButton.visibility = View.GONE
                        prevButton.visibility = View.GONE
                    }
                } else {
                    throw Exception(
                        getString(
                            R.string.failed_to_fetch_data_response_code,
                            contributorsConnection.responseCode.toString()
                        )
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    noInternetText.text =
                        getString(R.string.failed_to_load_data, e.localizedMessage)
                    noInternetText.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun fetchContributorName(username: String): String? {
        return try {
            val userDetailsConnection =
                URL(GITHUB_API_USER + username).openConnection() as HttpURLConnection
            userDetailsConnection.connectTimeout = 5000
            userDetailsConnection.readTimeout = 5000
            userDetailsConnection.requestMethod = "GET"

            if (userDetailsConnection.responseCode == HttpURLConnection.HTTP_OK) {
                val response =
                    userDetailsConnection.inputStream.bufferedReader().use { it.readText() }
                val userDetails = JSONObject(response)
                userDetails.optString("name")
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun displayPage() {
        // Ambil data untuk halaman saat ini
        val startIndex = currentPage * pageSize
        val endIndex = minOf((currentPage + 1) * pageSize, contributorsModelArrayList.size)
        val pageData = contributorsModelArrayList.subList(startIndex, endIndex)

        // Set adapter untuk recycler view
        recyclerView.adapter = ContributorsAdapter(requireContext(), pageData, this)
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        // Mengatur tombol Next dan Prev apakah aktif atau tidak
        prevButton.isEnabled = currentPage > 0
        nextButton.isEnabled = currentPage < totalPages - 1
    }

    private fun showNoInternetMessage() {
        noInternetText.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    //Visit Profile
    override fun onItemClick(data: ContributorsModel) {
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
                openURL.data = Uri.parse("https://github.com/" + data.username + "/")
                startActivity(openURL)
            }
            .show()

    }


    override fun onResume() {

        super.onResume()

        betaTester?.isVisible = isInstalledGooglePlay
    }

}