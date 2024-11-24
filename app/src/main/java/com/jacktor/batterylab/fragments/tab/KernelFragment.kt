package com.jacktor.batterylab.fragments.tab

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.jacktor.batterylab.R
import com.jacktor.batterylab.adapters.KernelAdapter
import com.jacktor.batterylab.databinding.KernelFragmentBinding
import com.jacktor.batterylab.interfaces.KernelInterface
import com.jacktor.batterylab.interfaces.RecyclerKernelCheckedChangeListener
import com.jacktor.batterylab.interfaces.RecyclerKernelClickListener
import com.jacktor.batterylab.utilities.Prefs
import com.jacktor.batterylab.utilities.RootUtils
import com.jacktor.batterylab.views.KernelModel
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KernelFragment : Fragment(R.layout.kernel_fragment), KernelInterface,
    RecyclerKernelClickListener, RecyclerKernelCheckedChangeListener {

    private lateinit var binding: KernelFragmentBinding
    private val data = mutableListOf<KernelModel>()
    private var adapter: KernelAdapter? = null
    private var pref: Prefs? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    companion object {
        var instance: KernelFragment? = null
            private set
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = KernelFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        instance = this
        pref = Prefs(requireContext())

        setupRecyclerView()
        setupRefreshListeners()

        if (isRootAccessAvailable() && requestRootAccess()) {
            shellDialog(
                requireContext(),
                getString(R.string.experiment),
                "root:~# ls -p /sys/class/power_supply/battery | grep -v /\n\n"
                        + getString(R.string.get_kernel_information)
            )

            Handler(Looper.getMainLooper()).postDelayed(
                {
                    coroutineScope.launch {
                        kernelInformation()
                    }

                }, 1000
            )
        } else {
            displayNoRootAccessUI()
        }
    }

    private fun setupRecyclerView() {
        adapter = KernelAdapter(data, requireContext(), this, this)
        binding.recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = this@KernelFragment.adapter
        }
    }

    private fun setupRefreshListeners() {
        binding.refreshKernel.setOnRefreshListener {
            coroutineScope.launch {
                kernelInformation()
                binding.refreshKernel.isRefreshing = false
            }
        }

        binding.refreshNoroot.setOnRefreshListener {
            coroutineScope.launch {
                kernelInformation()
                binding.refreshNoroot.isRefreshing = false
            }
        }
    }

    private fun displayNoRootAccessUI() {
        binding.refreshKernel.visibility = View.GONE
        binding.refreshNoroot.visibility = View.VISIBLE
    }

    @SuppressLint("NotifyDataSetChanged")
    suspend fun kernelInformation() {
        val hasRoot = isRootAccessAvailable() && requestRootAccess()

        if (hasRoot) {
            binding.refreshKernel.visibility = View.VISIBLE
            binding.refreshNoroot.visibility = View.GONE

            val output = withContext(Dispatchers.IO) {
                Shell.cmd("ls -p /sys/class/power_supply/battery | grep -v /").exec().out
            }

            val updatedData = output.map { filename ->
                KernelModel(
                    "$filename: ${getKernelData(filename)}",
                    getString(
                        R.string.stored_value,
                        getKernelValueFromFile(requireContext(), filename)
                            ?: getString(R.string.none)
                    ),
                    getString(
                        R.string.kernel_status, if (getKernelValueFromFile(
                                requireContext(), filename
                            ) != null
                        ) getString(R.string.enabled) else getString(R.string.disabled)
                    ),
                    getString(
                        R.string.kernel_available,
                        if (getKernelData(filename) != "-") getString(R.string.yes) else getString(
                            R.string.no
                        )
                    ),
                    if (getKernelData(filename) != "-") 0f else 5f,
                    getKernelValueFromFile(requireContext(), filename) != null
                )
            }

            // Update hanya jika ada perubahan
            if (data != updatedData) {
                data.clear()
                data.addAll(updatedData)
                adapter?.notifyDataSetChanged()
            }
        } else {
            displayNoRootAccessUI()
        }
    }

    fun updateKernelInformation(position: Int, filename: String) {
        coroutineScope.launch {
            val updatedModel = KernelModel(
                "$filename: ${getKernelData(filename)}",
                getString(
                    R.string.stored_value,
                    getKernelValueFromFile(requireContext(), filename)
                        ?: getString(R.string.none)
                ),
                getString(
                    R.string.kernel_status, if (getKernelValueFromFile(
                            requireContext(), filename
                        ) != null
                    ) getString(R.string.enabled) else getString(R.string.disabled)
                ),
                getString(
                    R.string.kernel_available,
                    if (getKernelData(filename) != "-") getString(R.string.yes) else getString(
                        R.string.no
                    )
                ),
                if (getKernelData(filename) != "-") 0f else 5f,
                getKernelValueFromFile(requireContext(), filename) != null
            )
            data[position] = updatedModel
            adapter?.notifyItemChanged(position)
        }
    }

    override fun onItemClick(data: KernelModel, position: Int) {
        val filename = data.list.substringBefore(": ")
        val kernelValue = data.list.substringAfter(": ")
        changeKernelValue(kernelValue, filename, position, requireContext())
    }

    override fun onItemChecked(isChecked: Boolean, data: KernelModel, position: Int) {
        val filename = data.list.substringBefore(": ")
        if (!isChecked) deleteKernelCmd(requireContext(), filename)
        updateKernelInformation(position, filename)
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        instance = null
        super.onDestroy()
    }

    private fun isRootAccessAvailable() = RootUtils.hasRootAccess()
    private fun requestRootAccess() = RootUtils.reqRootAccess()

}
