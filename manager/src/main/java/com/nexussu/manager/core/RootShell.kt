package com.nexussu.manager.core

import java.io.BufferedReader
import java.io.InputStreamReader

object RootShell {
    /**
     * Executes a command as root and returns the output string.
     * Works because the kernel hook intercepts 'su' and grants us UID 0.
     */
    fun execute(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()
            output.trim()
        } catch (e: Exception) {
            "Error"
        }
    }

    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun getKernelVersion(): String {
        return execute("uname -r")
    }

    fun getSelinuxStatus(): String {
        val status = execute("getenforce")
        return if (status.equals("Enforcing", ignoreCase = true)) "Enforcing" else "Permissive"
    }
}
