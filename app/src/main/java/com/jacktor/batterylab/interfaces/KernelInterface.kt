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
import com.jacktor.batterylab.fragments.tab.KernelFragment
import com.jacktor.batterylab.utilities.Constants.SCRIPT_FILE_NAME
import com.jacktor.batterylab.utilities.Prefs
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

interface KernelInterface {

    fun getKernelData(filename: String): String =
        Shell.cmd("cat /sys/class/power_supply/battery/$filename").exec().out.firstOrNull() ?: "-"

    fun changeKernelValue(
        value: String, filename: String, index: Int, context: Context, designCapacity: Prefs? = null
    ) {
        val binding =
            ChangeKernelValueDialogBinding.inflate(LayoutInflater.from(context), null, false)
                .apply {
                    changeKernelValue.setText(value)
                }

        MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setPositiveButton(context.getString(R.string.change)) { _, _ ->
                editKernelData(binding.changeKernelValue.text.toString(), filename, index, context)
                saveCommand(filename, binding.changeKernelValue.text.toString(), context)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create().apply {
                setDialogShowListener(this, binding.changeKernelValue)
                show()
            }
    }

    private fun setDialogShowListener(dialog: AlertDialog, inputField: TextInputEditText) {
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
            inputField.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = s.isNotEmpty()
                }
            })
        }
    }

    fun editKernelData(kernelValue: String, filename: String, index: Int, context: Context) {
        val commands = listOf(
            "chmod 0666 /sys/class/power_supply/battery/$filename",
            "echo $kernelValue > /sys/class/power_supply/battery/$filename",
            "chmod 0444 /sys/class/power_supply/battery/$filename"
        )
        Shell.cmd(commands.joinToString(" && ")).exec()
        KernelFragment.instance?.updateKernelInformation(index, filename)
    }

    private fun saveCommand(filename: String, value: String, context: Context) {
        val file = File(context.filesDir, SCRIPT_FILE_NAME)
        val currentContent = file.readTextOrEmpty()

        val command = "echo $value > /sys/class/power_supply/battery/$filename"
        val commands = """
            chmod 0666 /sys/class/power_supply/battery/$filename
            $command
            chmod 0444 /sys/class/power_supply/battery/$filename
        """.trimIndent()

        when {
            !file.exists() -> file.writeText("#!/bin/bash\n\n$commands")
            currentContent.contains(command) -> {
                val updatedContent = currentContent.replace(
                    Regex("echo .* > /sys/class/power_supply/battery/$filename"),
                    command
                )
                file.writeText(updatedContent)
            }

            else -> file.appendText("\n\n$commands")
        }
    }

    fun getKernelValueFromFile(context: Context, filename: String): String? {
        val content = File(context.filesDir, SCRIPT_FILE_NAME).readTextOrEmpty()
        val regex = Regex("""echo\s+(\d+)\s+>""")
        return regex.find(content.substringAfter(filename))?.groupValues?.get(1)
    }

    fun deleteKernelCmd(context: Context, filename: String) {
        val file = File(context.filesDir, SCRIPT_FILE_NAME)
        if (file.exists()) {
            val updatedContent = file.readTextOrEmpty().lines()
                .filterNot { it.contains("/sys/class/power_supply/battery/$filename") }
                .joinToString("\n").trimEnd()
            file.writeText(updatedContent)
        }
    }

    fun shellDialog(
        context: Context,
        title: String? = null,
        message: String? = null,
        type: Int = 1
    ) {
        val binding = ShellDialogBinding.inflate(LayoutInflater.from(context), null, false).apply {
            script.setText(message ?: ">_")
        }

        MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_terminal_24dp))
            .setTitle(title?.uppercase() ?: "Jacktor Shell")
            .setCancelable(type != 1)
            .apply { if (type != 1) setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() } }
            .create().apply {
                if (type == 1) Handler(Looper.getMainLooper()).postDelayed({ dismiss() }, 3000)
                show()
            }
    }

    companion object {
        fun getKernelFromFile(context: Context): String =
            File(context.filesDir, SCRIPT_FILE_NAME).readTextOrEmpty()

        fun executeShellFile(context: Context) {
            val scriptPath = "${context.filesDir}/$SCRIPT_FILE_NAME"
            Shell.cmd("su && sh $scriptPath").exec().apply {
                if (isSuccess) {
                    shellDialog(
                        context,
                        context.getString(R.string.experiment),
                        "su && sh $SCRIPT_FILE_NAME\n\n${context.getString(R.string.executes_and_update_info)}"
                    )
                    Handler(Looper.getMainLooper()).postDelayed({
                        CoroutineScope(Dispatchers.Main).launch {
                            KernelFragment.instance?.kernelInformation()
                        }
                    }, 1000)
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
            shellDialog(context, title, message, type)
        }

        fun resetScript(context: Context) {
            val file = File(context.filesDir, SCRIPT_FILE_NAME)
            val messageRes = if (file.exists() || file.createNewFile()) {
                file.writeText("#!/bin/bash")
                R.string.script_has_been_reset
            } else {
                R.string.command_file_is_not_available
            }
            Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
        }
    }
}

// Extension function for reading text from a file safely
private fun File.readTextOrEmpty(): String = try {
    this.readText()
} catch (e: IOException) {
    ""
}
