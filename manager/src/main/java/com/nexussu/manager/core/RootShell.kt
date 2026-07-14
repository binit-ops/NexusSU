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
        } catch (e: Exception) { "Error" }
    }

    fun executeBoolean(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) { false }
    }

    fun isRootAvailable(): Boolean = executeBoolean("id")
    fun getKernelVersion(): String = execute("uname -r")
    fun getSelinuxStatus(): String = execute("getenforce")

        // NEW: Get real Verified Boot state
    fun getVerifiedBootState(): String {
        val state = execute("getprop ro.boot.verifiedbootstate").trim()
        return when (state) {
            "green" -> "Green (Locked)"
            "yellow" -> "Yellow (Locked Custom)"
            "orange" -> "Orange (Unlocked)"
            "red" -> "Red (Unverified)"
            else -> "Unknown ($state)"
        }
    }
    
        fun installModule(zipPath: String): Boolean {
        val cmd = """
            mkdir -p /data/adb/nexussu/modules_temp
            unzip -o $zipPath -d /data/adb/nexussu/modules_temp
            if [ -f /data/adb/nexussu/modules_temp/module.prop ]; then
                ID=$(grep '^id=' /data/adb/nexussu/modules_temp/module.prop | cut -d= -f2)
                mkdir -p /data/adb/nexussu/modules/$ID
                cp -r /data/adb/nexussu/modules_temp/* /data/adb/nexussu/modules/$ID/
                rm -rf /data/adb/nexussu/modules_temp
                
                # NEW: Execute install.sh if it exists
                if [ -f /data/adb/nexussu/modules/$ID/install.sh ]; then
                    chmod 0755 /data/adb/nexussu/modules/$ID/install.sh
                    # Set up Magisk-style environment variables so the script knows where it is
                    export MODPATH=/data/adb/nexussu/modules/$ID
                    export ZIPFILE=$zipPath
                    sh /data/adb/nexussu/modules/$ID/install.sh
                fi
                
                # Mount the system files if installation was successful
                if [ -d /data/adb/nexussu/modules/$ID/system ]; then
                    find /data/adb/nexussu/modules/$ID/system -type f | while read file; do
                        target_path="/system${'$'}{file#/data/adb/nexussu/modules/$ID/system}"
                        mkdir -p ${'$'}(dirname ${'$'}target_path)
                        mount --bind ${'$'}file ${'$'}target_path
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

    fun setModuleEnabled(id: String, enabled: Boolean): Boolean {
        val basePath = "/data/adb/nexussu/modules/$id"
        val cmd = if (enabled) {
            """
            rm -f $basePath/disable
            if [ -d $basePath/system ]; then
                find $basePath/system -type f | while read file; do
                    target_path="/system\${file#$basePath/system}"
                    mkdir -p \$(dirname \$target_path)
                    mount --bind \$file \$target_path
                done
            fi
            echo "SUCCESS"
            """.trimIndent()
        } else {
            """
            touch $basePath/disable
            if [ -d $basePath/system ]; then
                find $basePath/system -type f | while read file; do
                    target_path="/system\${file#$basePath/system}"
                    umount \$target_path 2>/dev/null
                done
            fi
            echo "SUCCESS"
            """.trimIndent()
        }
        return execute(cmd).contains("SUCCESS")
    }

    // NEW: Delete Module Logic
    fun deleteModule(id: String): Boolean {
        val basePath = "/data/adb/nexussu/modules/$id"
        val cmd = """
            if [ -d $basePath/system ]; then
                find $basePath/system -type f | while read file; do
                    target_path="/system\${file#$basePath/system}"
                    umount \$target_path 2>/dev/null
                done
            fi
            rm -rf $basePath
            echo "SUCCESS"
        """.trimIndent()
        return execute(cmd).contains("SUCCESS")
    }
}
