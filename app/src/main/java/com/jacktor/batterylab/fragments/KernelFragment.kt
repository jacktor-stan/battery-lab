package com.jacktor.batterylab.fragments

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
import com.jacktor.batterylab.interfaces.views.MenuInterface
import com.jacktor.batterylab.utilities.Prefs
import com.jacktor.batterylab.views.KernelModel
import com.jacktor.rootchecker.RootChecker
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds


class KernelFragment : Fragment(R.layout.kernel_fragment), MenuInterface, KernelInterface,
    RecyclerKernelClickListener, RecyclerKernelCheckedChangeListener {

    private lateinit var binding: KernelFragmentBinding

    //private var job: Job? = null
    //private var isJob = false
    private var data = ArrayList<KernelModel>()
    private var adapter: KernelAdapter? = null
    private var pref: Prefs? = null
    private var rootChecker: RootChecker? = null

    companion object {
        var instance: KernelFragment? = null
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        binding = KernelFragmentBinding.inflate(inflater, container, false)

        return binding.root.rootView
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        pref = Prefs(requireContext())
        rootChecker = RootChecker(requireContext())

        if (rootChecker!!.isRooted && Shell.cmd("su").exec().isSuccess) {
            shellDialog(
                requireContext(), getString(R.string.kernel),
                "root:~# ls -p /sys/class/power_supply/battery | grep -v /\n\n"
                        + getString(R.string.get_kernel_information)
            )
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    kernelInformation()

                }, 1000
            )

            adapter = KernelAdapter(
                data,
                requireContext(),
                this as RecyclerKernelClickListener,
                this as RecyclerKernelCheckedChangeListener
            )

            binding.recyclerview.setHasFixedSize(true)
            binding.recyclerview.layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.VERTICAL, false
            )
            binding.recyclerview.adapter = adapter
        } else kernelInformation()


        binding.refreshKernel.setOnRefreshListener {
            CoroutineScope(Dispatchers.Main).launch {
                delay(2.seconds)
                kernelInformation()
            }
        }

        binding.refreshNoroot.setOnRefreshListener {
            CoroutineScope(Dispatchers.Main).launch {
                delay(1.seconds)
                kernelInformation()
            }
        }
    }


    /*private fun kernelInformationJob() {

        if (pref?.getBoolean("realtime_kernel", resources.getBoolean(R.bool.realtime_kernel))!!) {
            if (job == null) job = CoroutineScope(Dispatchers.Default).launch {
                while (isJob) {

                    withContext(Dispatchers.Main) {

                        kernelInformation()
                        delay(1500L)
                    }
                }
            }
        }
    }*/

    @SuppressLint("NotifyDataSetChanged")
    fun kernelInformation() {
        var output: ArrayList<String>

        //Periksa akses root
        if (rootChecker!!.isRooted && Shell.cmd("su").exec().isSuccess) {

            //Ambil list kernel
            binding.refreshKernel.visibility = View.VISIBLE
            binding.refreshNoroot.visibility = View.GONE

            binding.refreshKernel.isRefreshing = false

            output = ArrayList()
            val result = Shell.cmd("ls -p /sys/class/power_supply/battery | grep -v /").exec()
            if (result.out.size != 0) {
                output = result.out as ArrayList<String>
            }

            val arrayList: ArrayList<String> = output
            data.clear()
            for (i in arrayList.indices) {
                data.add(
                    KernelModel(
                        arrayList[i] + ": " + getKernelData(arrayList[i]),
                        getString(
                            R.string.stored_value,
                            getKernelValueFromFile(requireContext(), arrayList[i])
                                ?: getString(R.string.none)
                        ),
                        getString(
                            R.string.kernel_status, if (getKernelValueFromFile(
                                    requireContext(), arrayList[i]
                                ) != null
                            ) getString(R.string.enabled) else getString(R.string.disabled)
                        ),
                        getString(
                            R.string.kernel_available,
                            if (getKernelData(arrayList[i]) != "-") getString(R.string.yes) else getString(
                                R.string.no
                            )
                        ),
                        if (getKernelData(arrayList[i]) != "-") 0f else 5f,
                        getKernelValueFromFile(requireContext(), arrayList[i]) != null
                    )
                )
            }

            adapter?.notifyDataSetChanged()
        } else {
            binding.refreshKernel.visibility = View.GONE
            binding.refreshNoroot.visibility = View.VISIBLE

            binding.refreshNoroot.isRefreshing = false

            if (rootChecker!!.isRooted) {
                if (!Shell.cmd("su").exec().isSuccess) {
                    binding.rootMsg.text = requireContext().getString(R.string.root_access_info_1)
                }
            } else {
                binding.rootMsg.text = requireContext().getString(R.string.root_access_info_0)
            }
        }
    }


    fun updateKernelInformation(position: Int, filename: String) {

        Handler(Looper.getMainLooper()).postDelayed(
            {
                data[position] = KernelModel(
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

                adapter?.notifyItemChanged(position)
            }, 500
        )
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


    override fun onResume() {

        super.onResume()

        if (instance == null) instance = this

        //isJob = true
        //kernelInformationJob()
    }


    /*override fun onStop() {

        super.onStop()

        isJob = false
        job?.cancel()
        job = null
    }*/

    override fun onDestroy() {

        instance = null
        //isJob = false
        //job?.cancel()
        //job = null

        super.onDestroy()
    }
}