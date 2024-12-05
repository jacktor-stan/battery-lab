package com.jacktor.batterylab.interfaces

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textview.MaterialTextView
import com.jacktor.batterylab.R
import com.jacktor.batterylab.adapters.ContributorsAdapter
import com.jacktor.batterylab.utilities.Constants.BACKEND_API_CONTRIBUTORS
import com.jacktor.batterylab.views.ContributorsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

interface ContributorsInterface {
    fun onItemClick(data: ContributorsModel)

    companion object {
        private lateinit var recyclerView: RecyclerView
        private lateinit var noInternetText: MaterialTextView
        private lateinit var nextButton: MaterialButton
        private lateinit var prevButton: MaterialButton
        private lateinit var progressBar: LinearProgressIndicator
        private var contributorsModelArrayList: ArrayList<ContributorsModel> = ArrayList()

        private var currentPage = 0
        private var pageSize = 5
        private var totalPages = 0
    }

    fun showContributorsDialog(context: Context) {
        val dialog = MaterialAlertDialogBuilder(context, R.style.ContributorsDialog)
        val inflater = LayoutInflater.from(context)
        val customView = inflater.inflate(R.layout.contributors_dialog, null)

        recyclerView = customView.findViewById(R.id.contributors_recycler)
        noInternetText = customView.findViewById(R.id.no_internet_text)
        nextButton = customView.findViewById(R.id.next_button)
        prevButton = customView.findViewById(R.id.prev_button)
        progressBar = customView.findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(context)

        // Bersihkan list agar tidak ada data ganda
        contributorsModelArrayList.clear()

        // Periksa jika cache sudah kadaluwarsa
        val cacheFile = getCacheFile(context)
        if (isCacheExpired(cacheFile)) {
            clearCache(context)
        }

        // Jika data tersedia di cache, gunakan cache
        val cachedData = readFromCache(context)
        if (cachedData != null) {
            parseContributorsFromCache(cachedData)
            totalPages = (contributorsModelArrayList.size + pageSize - 1) / pageSize
            displayPage(context)
        } else if (isNetworkAvailable(context)) {
            fetchContributors(context)
        } else {
            showNoInternetMessage()
        }

        dialog.setView(customView)
            .setTitle(context.getString(R.string.contributors))
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.close)) { dialogBtn, _ -> dialogBtn.dismiss() }
            .show()

        prevButton.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                displayPage(context)
            }
        }

        nextButton.setOnClickListener {
            if (currentPage < totalPages - 1) {
                currentPage++
                displayPage(context)
            }
        }
    }

    private fun fetchContributors(context: Context) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contributorsConnection =
                    URL(BACKEND_API_CONTRIBUTORS).openConnection() as HttpURLConnection
                contributorsConnection.connectTimeout = 5000
                contributorsConnection.readTimeout = 5000
                contributorsConnection.requestMethod = "GET"

                if (contributorsConnection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response =
                        contributorsConnection.inputStream.bufferedReader().use { it.readText() }

                    // Simpan ke cache
                    saveToCache(context, response)
                    parseContributorsFromCache(response)

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        currentPage = 0
                        displayPage(context)
                    }

                    if (contributorsModelArrayList.size > pageSize) {
                        nextButton.visibility = View.VISIBLE
                        prevButton.visibility = View.VISIBLE
                    } else {
                        nextButton.visibility = View.GONE
                        prevButton.visibility = View.GONE
                    }
                } else {
                    throw Exception(
                        context.getString(
                            R.string.failed_to_fetch_data_response_code,
                            contributorsConnection.responseCode.toString()
                        )
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    noInternetText.text =
                        context.getString(R.string.failed_to_load_data, e.localizedMessage)
                    noInternetText.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun parseContributorsFromCache(response: String) {
        val contributorsArray = JSONArray(response)

        for (i in 0 until contributorsArray.length()) {
            val contributor = contributorsArray.getJSONObject(i)
            val username = contributor.getString("login")
            val avatarUrl = contributor.optString("avatar_url", "")
            val contributions = contributor.optInt("contributions", 0)
            val name = contributor.optString("name", username)
            val htmlUrl = contributor.optString("html_url", "")

            contributorsModelArrayList.add(
                ContributorsModel(
                    name = name,
                    username = username,
                    avatarUrl = avatarUrl,
                    contributions = contributions,
                    htmlUrl = htmlUrl
                )
            )
        }

        contributorsModelArrayList.sortByDescending { it.contributions }
    }


    private fun displayPage(context: Context) {
        val startIndex = currentPage * pageSize
        val endIndex = minOf((currentPage + 1) * pageSize, contributorsModelArrayList.size)
        val pageData = contributorsModelArrayList.subList(startIndex, endIndex)

        recyclerView.adapter = ContributorsAdapter(
            context, pageData,
            this
        )
        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        prevButton.isEnabled = currentPage > 0
        nextButton.isEnabled = currentPage < totalPages - 1
    }

    private fun showNoInternetMessage() {
        noInternetText.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


    private fun getCacheFile(context: Context): File {
        return File(context.cacheDir, "contributors.json")
    }

    private fun isCacheExpired(cacheFile: File): Boolean {
        if (!cacheFile.exists()) return true
        val lastModified = cacheFile.lastModified()
        val oneDayInMillis = TimeUnit.DAYS.toMillis(1)
        return System.currentTimeMillis() - lastModified > oneDayInMillis
    }

    private fun saveToCache(context: Context, data: String) {
        val cacheFile = getCacheFile(context)
        cacheFile.writeText(data)
    }

    private fun readFromCache(context: Context): String? {
        val cacheFile = getCacheFile(context)
        return if (cacheFile.exists()) cacheFile.readText() else null
    }

    private fun clearCache(context: Context) {
        // Hapus file cache JSON
        getCacheFile(context).delete()

        // Bersihkan cache gambar Picasso
        try {
            val cacheDir = File(context.cacheDir, "picasso-cache")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}