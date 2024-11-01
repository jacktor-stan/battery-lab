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
import java.io.FileInputStream
import java.io.IOException


interface KernelInterface {
    fun getKernelData(filename: String): String {
        var getKernelDataResult = "-"

        val result = Shell.cmd("cat /sys/class/power_supply/battery/$filename").exec()
        if (result.out.size != 0) {
            getKernelDataResult = result.out[0]
        }
        return getKernelDataResult
    }


    fun changeKernelValue(
        value: String, filename: String, index: Int, context: Context, designCapacity: Prefs? = null
    ) {

        //val pref = Prefs(context)

        val dialog = MaterialAlertDialogBuilder(context)

        val binding = ChangeKernelValueDialogBinding.inflate(
            LayoutInflater.from(context), null, false
        )

        dialog.setView(binding.root.rootView)

        binding.changeKernelValue.setText(value)

        dialog.setPositiveButton(context.getString(R.string.change)) { _, _ ->

            editKernelData(binding.changeKernelValue.text.toString(), filename, index, context)
            saveCommand(filename, binding.changeKernelValue.text.toString(), context)
            //Toast.makeText(context, filename ,Toast.LENGTH_SHORT).show()

        }

        dialog.setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }

        val dialogCreate = dialog.create()

        changeDesignCapacityDialogCreateShowListener(
            context, dialogCreate, binding.changeKernelValue
        )

        dialogCreate.show()
    }

    private fun changeDesignCapacityDialogCreateShowListener(
        context: Context, dialogCreate: AlertDialog, changeDesignCapacity: TextInputEditText
    ) {

        dialogCreate.setOnShowListener {

            dialogCreate.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false

            changeDesignCapacity.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(
                    s: CharSequence, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                    dialogCreate.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = try {
                        s.isNotEmpty()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(
                            context, e.message ?: e.toString(), Toast.LENGTH_LONG
                        ).show()
                        false
                    }
                }
            })
        }
    }


    fun editKernelData(kernelValue: String, filename: String, index: Int, context: Context) {

        val shellCommands = arrayOf(
            "chmod 0666 /sys/class/power_supply/battery/$filename",
            "echo $kernelValue > sys/class/power_supply/battery/$filename",
            "chmod 0444 /sys/class/power_supply/battery/$filename"
        )

        /*MaterialAlertDialogBuilder(context).apply {
            setIcon(
                AppCompatResources.getDrawable(
                    context, R.drawable.ic_terminal_24dp
                )
            )
            setTitle(context.getString(R.string.jacktor_shell))
            setMessage(context.getString(R.string.starting_command_execution_wait))
            setCancelable(false)
        }.create().apply {
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    dismiss()
                }, 3000
            )
            show()
        }*/

        Shell.cmd(shellCommands.joinToString(separator = " && ")).exec()
        KernelFragment.instance?.updateKernelInformation(index, filename)
        //KernelFragment.instance?.kernelInformation()
    }

    private fun saveCommand(filename: String, value: String, context: Context) {

        val file = File(context.filesDir.path, SCRIPT_FILE_NAME)
        var content = ""

        try {
            val inputStream = FileInputStream(file)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            content = String(buffer)

        } catch (e: IOException) {
            e.printStackTrace()
        }

        val arrayCommands = arrayOf(
            "chmod 0666 /sys/class/power_supply/battery/$filename",
            "echo $value > sys/class/power_supply/battery/$filename",
            "chmod 0444 /sys/class/power_supply/battery/$filename"
        )

        // create a new file
        if (file.createNewFile() && !file.exists()) {
            Companion.shellDialog(context, context.getString(R.string.create_and_saving_scripts))
            file.writeText("#!/bin/bash\n\n" + arrayCommands.joinToString(separator = " && "))
        } else {

            // Dapatkan nilai dari file command.sh
            val findBefore =
                content.indexOf("/sys/class/power_supply/battery/$filename && echo").let {
                    if (it == -1) null else content.substring(it + 32)
                        .replace("$filename && echo ", "").split("/")[0]
                        .replace(" > sys", "")
                }

            // Cek jika duplikat jangan tulis lagi (ganti nilai saja)
            if (content.contains("> sys/class/power_supply/battery/$filename")) {
                //Companion.shellDialog(context, context.getString(R.string.changing_script_value))
                file.writeText(
                    content.replace(
                        "$findBefore > sys/class/power_supply/battery/$filename",
                        "$value > sys/class/power_supply/battery/$filename"
                    )
                )
            } else {
                //Companion.shellDialog(context, context.getString(R.string.saving_script))
                file.appendText("\n\n" + arrayCommands.joinToString(separator = " && "))
            }
        }
    }

    fun getKernelValueFromFile(context: Context, filename: String): String? {

        val file = File(context.filesDir.path, SCRIPT_FILE_NAME)
        var content = ""

        try {
            val inputStream = FileInputStream(file)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            content = String(buffer)

        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Dapatkan nilai dari file command.sh
        val findValue = content.indexOf("/sys/class/power_supply/battery/$filename && echo").let {
            if (it == -1) null else content.substring(it + 32).replace("$filename && echo ", "")
                .split("/")[0].replace(" > sys", "")
        }

        return findValue
    }

    fun deleteKernelCmd(context: Context, filename: String) {
        val file = File(context.filesDir.path, SCRIPT_FILE_NAME)
        var content = ""

        try {
            val inputStream = FileInputStream(file)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            content = String(buffer)

        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Dapatkan nilai dari file command.sh
        val findValue = content.indexOf("/sys/class/power_supply/battery/$filename && echo").let {
            if (it == -1) null else content.substring(it + 32).replace("$filename && echo ", "")
                .split("/")[0].replace(" > sys", "")
        }

        val arrayCommands = arrayOf(
            "chmod 0666 /sys/class/power_supply/battery/$filename",
            "echo $findValue > sys/class/power_supply/battery/$filename",
            "chmod 0444 /sys/class/power_supply/battery/$filename"
        )

        file.writeText(
            content.replace(
                "\n\n" + arrayCommands.joinToString(separator = " && "), ""
            )
        )
    }

    fun shellDialog(
        context: Context,
        title: String? = null,
        message: String? = null,
        type: Int = 1
    ) {
        val binding = ShellDialogBinding.inflate(
            LayoutInflater.from(context), null, false
        )
        val dialog = MaterialAlertDialogBuilder(context)

        dialog.setView(binding.root.rootView)
        dialog.apply {
            setIcon(
                AppCompatResources.getDrawable(
                    context, R.drawable.ic_terminal_24dp
                )
            )
            setTitle(title?.uppercase() ?: "jacktor Shell")

            if (type == 1) {
                binding.script.setText(message ?: ">_")
                setCancelable(false)
            } else {
                binding.script.setText(message)
                setCancelable(true)
                setPositiveButton(R.string.ok) { d, _ -> d.dismiss() }
            }

        }.create().apply {
            if (type == 1) {
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        dismiss()
                    }, 3000
                )
            }
            show()
        }
    }


    companion object {
        fun getKernelFromFile(context: Context): String {

            val file = File(context.filesDir.path, SCRIPT_FILE_NAME)
            var content = ""

            try {
                val inputStream = FileInputStream(file)
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                content = String(buffer)

            } catch (e: IOException) {
                e.printStackTrace()
            }

            return content
        }

        fun executeShellFile(context: Context) {
            Shell.cmd("su && sh ${context.filesDir.path}/$SCRIPT_FILE_NAME").exec().apply {
                if (isSuccess) {
                    shellDialog(
                        context, context.getString(R.string.experiment),
                        "su && sh $SCRIPT_FILE_NAME\n\n" + context.getString(R.string.executes_and_update_info)
                    )

                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            KernelFragment.instance?.kernelInformation()
                        }, 1000
                    )
                } else shellDialog(
                    context,
                    context.getString(R.string.experiment),
                    context.getString(R.string.execute_failed),
                    0
                )
            }
        }

        fun shellDialog(
            context: Context,
            title: String? = null,
            message: String? = null,
            type: Int = 1
        ) {
            val binding = ShellDialogBinding.inflate(
                LayoutInflater.from(context), null, false
            )
            val dialog = MaterialAlertDialogBuilder(context)

            dialog.setView(binding.root.rootView)
            dialog.apply {
                setIcon(
                    AppCompatResources.getDrawable(
                        context, R.drawable.ic_terminal_24dp
                    )
                )
                setTitle(title?.uppercase() ?: "Jacktor Shell")

                if (type == 1) {
                    binding.script.setText(message ?: ">_")
                    setCancelable(false)
                } else {
                    binding.script.setText(message)
                    setCancelable(true)
                    setPositiveButton(R.string.ok) { d, _ -> d.dismiss() }
                }

            }.create().apply {
                if (type == 1) {
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            dismiss()
                        }, 3000
                    )
                }
                show()
            }
        }

        fun resetScript(context: Context) {
            val scriptFileName = SCRIPT_FILE_NAME
            val file = File(context.filesDir.path, scriptFileName)

            if (file.exists()) {
                file.writeText("#!/bin/bash")
                Toast.makeText(
                    context,
                    context.getString(R.string.script_has_been_reset),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                if (file.createNewFile()) {
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
                    )
                        .show()
                }
            }
        }
    }
}