package com.jacktor.batterylab.interfaces

import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.jacktor.batterylab.R
import com.jacktor.batterylab.databinding.ChangeKernelValueDialogBinding
import com.jacktor.batterylab.databinding.ShellDialogBinding
import com.jacktor.batterylab.fragments.KernelFragment
import com.jacktor.batterylab.utilities.Constants.SCRIPT_FILE_NAME
import com.jacktor.batterylab.utilities.Prefs
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.IOException

interface KernelInterface {
    fun getKernelData(filename: String): String {
        val result = Shell.cmd("cat /sys/class/power_supply/battery/$filename").exec()
        return if (result.out.isNotEmpty()) result.out[0] else "-"
    }

    fun changeKernelValue(
        value: String, filename: String, index: Int, context: Context, designCapacity: Prefs? = null
    ) {
        val dialog = MaterialAlertDialogBuilder(context)
        val binding =
            ChangeKernelValueDialogBinding.inflate(LayoutInflater.from(context), null, false)
        dialog.setView(binding.root)

        binding.changeKernelValue.setText(value)
        dialog.setPositiveButton(context.getString(R.string.change)) { _, _ ->
            editKernelData(binding.changeKernelValue.text.toString(), filename, index, context)
            saveCommand(filename, binding.changeKernelValue.text.toString(), context)
        }
        dialog.setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }

        val dialogCreate = dialog.create()
        changeDesignCapacityDialogCreateShowListener(
            dialogCreate,
            binding.changeKernelValue
        )
        dialogCreate.show()
    }

    private fun changeDesignCapacityDialogCreateShowListener(
        dialogCreate: AlertDialog, changeDesignCapacity: TextInputEditText
    ) {
        dialogCreate.setOnShowListener {
            dialogCreate.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
            changeDesignCapacity.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    dialogCreate.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled =
                        s.isNotEmpty()
                }
            })
        }
    }

    fun editKernelData(kernelValue: String, filename: String, index: Int, context: Context) {
        val shellCommands = arrayOf(
            "chmod 0666 /sys/class/power_supply/battery/$filename",
            "echo $kernelValue > /sys/class/power_supply/battery/$filename",
            "chmod 0444 /sys/class/power_supply/battery/$filename"
        )
        Shell.cmd(shellCommands.joinToString(" && ")).exec()
        KernelFragment.instance?.updateKernelInformation(index, filename)
    }

    private fun saveCommand(filename: String, value: String, context: Context) {
        val file = File(context.filesDir, SCRIPT_FILE_NAME)
        val content = file.readTextOrEmpty()

        val arrayCommands = arrayOf(
            "chmod 0666 /sys/class/power_supply/battery/$filename",
            "echo $value > /sys/class/power_supply/battery/$filename",
            "chmod 0444 /sys/class/power_supply/battery/$filename"
        )

        if (file.createNewFile() && !file.exists()) {
            file.writeText("#!/bin/bash\n\n" + arrayCommands.joinToString(" && "))
        } else if (content.contains("> /sys/class/power_supply/battery/$filename")) {
            file.writeText(
                content.replace(
                    Regex("echo .* > /sys/class/power_supply/battery/$filename"),
                    "echo $value > /sys/class/power_supply/battery/$filename"
                )
            )
        } else {
            file.appendText("\n\n" + arrayCommands.joinToString(" && "))
        }
    }

    fun getKernelValueFromFile(context: Context, filename: String): String? {
        val file = File(context.filesDir, SCRIPT_FILE_NAME)
        val content = file.readTextOrEmpty()

        return content.substringAfter("$filename && echo ", "")
            .substringBefore(" > sys")
            .takeIf { it.isNotEmpty() }
    }

    fun deleteKernelCmd(context: Context, filename: String) {
        val file = File(context.filesDir, SCRIPT_FILE_NAME)
        val content = file.readTextOrEmpty()
        val commandTemplate = "/sys/class/power_supply/battery/$filename && echo"

        file.writeText(content.replace("\n$commandTemplate.*".toRegex(), ""))
    }

    fun shellDialog(
        context: Context,
        title: String? = null,
        message: String? = null,
        type: Int = 1
    ) {
        val binding = ShellDialogBinding.inflate(LayoutInflater.from(context), null, false)
        val dialog = MaterialAlertDialogBuilder(context)
        dialog.setView(binding.root)
        dialog.apply {
            setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_terminal_24dp))
            setTitle(title?.uppercase() ?: "Jacktor Shell")
            binding.script.setText(message ?: ">_")
            setCancelable(type != 1)
            if (type != 1) setPositiveButton(R.string.ok) { d, _ -> d.dismiss() }
        }.create().apply {
            if (type == 1) Handler(Looper.getMainLooper()).postDelayed({ dismiss() }, 3000)
            show()
        }
    }

    companion object {
        fun getKernelFromFile(context: Context): String {
            val file = File(context.filesDir, SCRIPT_FILE_NAME)
            return file.readTextOrEmpty()
        }

        fun executeShellFile(context: Context) {
            Shell.cmd("su && sh ${context.filesDir}/$SCRIPT_FILE_NAME").exec().apply {
                if (isSuccess) {
                    shellDialog(
                        context,
                        context.getString(R.string.experiment),
                        "su && sh $SCRIPT_FILE_NAME\n\n" + context.getString(R.string.executes_and_update_info)
                    )
                    Handler(Looper.getMainLooper()).postDelayed(
                        { KernelFragment.instance?.kernelInformation() }, 1000
                    )
                } else {
                    shellDialog(
                        context,
                        context.getString(R.string.experiment),
                        context.getString(R.string.execute_failed),
                        0
                    )
                }
            }
        }

        private fun shellDialog(
            context: Context,
            title: String? = null,
            message: String? = null,
            type: Int = 1
        ) {
            val binding = ShellDialogBinding.inflate(LayoutInflater.from(context), null, false)
            val dialog = MaterialAlertDialogBuilder(context)
            dialog.setView(binding.root)
            dialog.apply {
                setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_terminal_24dp))
                setTitle(title?.uppercase() ?: "Jacktor Shell")
                binding.script.setText(message ?: ">_")
                setCancelable(type != 1)
                if (type != 1) setPositiveButton(R.string.ok) { d, _ -> d.dismiss() }
            }.create().apply {
                if (type == 1) Handler(Looper.getMainLooper()).postDelayed({ dismiss() }, 3000)
                show()
            }
        }

        fun resetScript(context: Context) {
            val file = File(context.filesDir, SCRIPT_FILE_NAME)
            if (file.exists()) {
                file.writeText("#!/bin/bash")
                Toast.makeText(
                    context,
                    context.getString(R.string.script_has_been_reset),
                    Toast.LENGTH_SHORT
                ).show()
            } else if (file.createNewFile()) {
                file.writeText("#!/bin/bash")
                Toast.makeText(
                    context,
                    context.getString(R.string.script_has_been_reset),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.command_file_is_not_available),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

// Fungsi ekstensi untuk membaca teks dari file, mengembalikan string kosong jika ada IOException
private fun File.readTextOrEmpty(): String {
    return try {
        this.readText()
    } catch (e: IOException) {
        ""
    }
}
