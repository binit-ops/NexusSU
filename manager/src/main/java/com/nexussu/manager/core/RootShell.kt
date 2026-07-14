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
            LOGFILE=/data/adb/nexussu/install.log
            echo "--- NexusSU Module Installation ---" > $LOGFILE
            echo "ZIP: $zipPath" >> $LOGFILE
            
            mkdir -p /data/adb/nexussu/modules_temp
            unzip -o $zipPath -d /data/adb/nexussu/modules_temp >> $LOGFILE 2>&1
            
            if [ -f /data/adb/nexussu/modules_temp/module.prop ]; then
                ID=$(grep '^id=' /data/adb/nexussu/modules_temp/module.prop | cut -d= -f2)
                mkdir -p /data/adb/nexussu/modules/$ID
                cp -r /data/adb/nexussu/modules_temp/* /data/adb/nexussu/modules/$ID/
                rm -rf /data/adb/nexussu/modules_temp
                
                export MODPATH=/data/adb/nexussu/modules/$ID
                export ZIPFILE=$zipPath
                
                # NEW: Execute customize.sh (Magisk standard) or install.sh
                if [ -f $MODPATH/customize.sh ]; then
                    chmod 0755 $MODPATH/customize.sh
                    echo "Executing customize.sh..." >> $LOGFILE
                    # Provide a dummy ui_print function so scripts don't crash
                    ui_print() { echo "[UI] $1" >> $LOGFILE; }
                    export -f ui_print
                    sh $MODPATH/customize.sh >> $LOGFILE 2>&1
                elif [ -f $MODPATH/install.sh ]; then
                    chmod 0755 $MODPATH/install.sh
                    echo "Executing install.sh..." >> $LOGFILE
                    sh $MODPATH/install.sh >> $LOGFILE 2>&1
                fi
                
                # Mount the system files if installation was successful
                if [ -d $MODPATH/system ]; then
                    find $MODPATH/system -type f | while read file; do
                        target_path="/system${'$'}{file#$MODPATH/system}"
                        mkdir -p ${'$'}(dirname ${'$'}target_path)
                        mount --bind ${'$'}file ${'$'}target_path 2>> $LOGFILE
                    done
                fi
                echo "SUCCESS"
            else
                rm -rf /data/adb/nexussu/modules_temp
                echo "FAILED: module.prop not found" >> $LOGFILE
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
