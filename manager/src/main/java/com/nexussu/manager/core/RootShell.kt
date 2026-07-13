package com.nexussu.manager.core

import java.io.BufferedReader
import java.io.InputStreamReader

object RootShell {
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

    fun executeBoolean(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun isRootAvailable(): Boolean = executeBoolean("id")
    fun getKernelVersion(): String = execute("uname -r")
    fun getSelinuxStatus(): String = execute("getenforce")
    
    fun installModule(zipPath: String): Boolean {
        val cmd = """
            mkdir -p /data/adb/nexussu/modules_temp
            unzip -o $zipPath -d /data/adb/nexussu/modules_temp
            if [ -f /data/adb/nexussu/modules_temp/module.prop ]; then
                ID=$(grep '^id=' /data/adb/nexussu/modules_temp/module.prop | cut -d= -f2)
                mkdir -p /data/adb/nexussu/modules/$ID
                cp -r /data/adb/nexussu/modules_temp/* /data/adb/nexussu/modules/$ID/
                rm -rf /data/adb/nexussu/modules_temp
                
                if [ -d /data/adb/nexussu/modules/$ID/system ]; then
                    find /data/adb/nexussu/modules/$ID/system -type f | while read file; do
                        target_path="/system${file#/data/adb/nexussu/modules/$ID/system}"
                        mkdir -p $(dirname $target_path)
                        mount --bind $file $target_path
                    done
                fi
                echo "SUCCESS"
            else
                rm -rf /data/adb/nexussu/modules_temp
                echo "FAILED"
            fi
        """.trimIndent()
        return execute(cmd).contains("SUCCESS")
    }
}
