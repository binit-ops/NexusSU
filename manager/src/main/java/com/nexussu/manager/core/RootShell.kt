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
            echo "--- NexusSU Module Installation ---" > ${'$'}LOGFILE
            echo "ZIP: ${'$'}zipPath" >> ${'$'}LOGFILE
            
            # Detect Device Environment
            API=${'$'}(getprop ro.build.version.sdk)
            ABI=${'$'}(getprop ro.product.cpu.abi)
            ABI2=${'$'}(getprop ro.product.cpu.abilist | cut -d, -f2)
            IS64BIT=false
            if [ "${'$'}ABI" = "arm64-v8a" ] || [ "${'$'}ABI" = "x86_64" ]; then IS64BIT=true; fi
            ARCH=arm
            if [ "${'$'}ABI" = "arm64-v8a" ]; then ARCH=arm64; fi
            if [ "${'$'}ABI" = "x86" ]; then ARCH=x86; fi
            if [ "${'$'}ABI" = "x86_64" ]; then ARCH=x64; fi
            
            # Export Magisk-Standard Environment Variables
            export MAGISK_VER=27000
            export MAGISK_VER_CODE=27000
            export API
            export ABI
            export ABI2
            export ARCH
            export IS64BIT
            export BOOTMODE=true
            
            echo "API: ${'$'}API | ARCH: ${'$'}ARCH | ABI: ${'$'}ABI | 64BIT: ${'$'}IS64BIT" >> ${'$'}LOGFILE
            
            mkdir -p /data/adb/nexussu/modules_temp
            unzip -o ${'$'}zipPath -d /data/adb/nexussu/modules_temp >> ${'$'}LOGFILE 2>&1
            
            if [ -f /data/adb/nexussu/modules_temp/module.prop ]; then
                ID=${'$'}(grep '^id=' /data/adb/nexussu/modules_temp/module.prop | cut -d= -f2)
                mkdir -p /data/adb/nexussu/modules/${'$'}ID
                cp -r /data/adb/nexussu/modules_temp/* /data/adb/nexussu/modules/${'$'}ID/
                rm -rf /data/adb/nexussu/modules_temp
                
                export MODPATH=/data/adb/nexussu/modules/${'$'}ID
                export ZIPFILE=${'$'}zipPath
                
                # Execute customize.sh (Magisk standard) or install.sh
                if [ -f ${'$'}MODPATH/customize.sh ]; then
                    chmod 0755 ${'$'}MODPATH/customize.sh
                    echo "Executing customize.sh..." >> ${'$'}LOGFILE
                    ui_print() { echo "[UI] ${'$'}1" >> ${'$'}LOGFILE; }
                    export -f ui_print
                    sh ${'$'}MODPATH/customize.sh >> ${'$'}LOGFILE 2>&1
                elif [ -f ${'$'}MODPATH/install.sh ]; then
                    chmod 0755 ${'$'}MODPATH/install.sh
                    echo "Executing install.sh..." >> ${'$'}LOGFILE
                    sh ${'$'}MODPATH/install.sh >> ${'$'}LOGFILE 2>&1
                fi
                
                # NEW: Mount the system files if installation was successful AND skip_mount is NOT present
                if [ -d ${'$'}MODPATH/system ] && [ ! -f ${'$'}MODPATH/skip_mount ]; then
                    find ${'$'}MODPATH/system -type f | while read file; do
                        target_path="/system${'$'}{file#${'$'}MODPATH/system}"
                        mkdir -p ${'$'}(dirname ${'$'}target_path)
                        mount --bind ${'$'}file ${'$'}target_path 2>> ${'$'}LOGFILE
                    done
                fi
                echo "SUCCESS"
            else
                rm -rf /data/adb/nexussu/modules_temp
                echo "FAILED: module.prop not found" >> ${'$'}LOGFILE
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
            if [ -d $basePath/system ] && [ ! -f $basePath/skip_mount ]; then
                find $basePath/system -type f | while read file; do
                    target_path="/system${'$'}{file#$basePath/system}"
                    mkdir -p ${'$'}(dirname ${'$'}target_path)
                    mount --bind ${'$'}file ${'$'}target_path
                done
            fi
            echo "SUCCESS"
            """.trimIndent()
        } else {
            """
            touch $basePath/disable
            if [ -d $basePath/system ]; then
                find $basePath/system -type f | while read file; do
                    target_path="/system${'$'}{file#$basePath/system}"
                    umount ${'$'}target_path 2>/dev/null
                done
            fi
            echo "SUCCESS"
            """.trimIndent()
        }
        return execute(cmd).contains("SUCCESS")
    }

    // NEW: Deferred uninstall (Magisk standard)
    fun deleteModule(id: String): Boolean {
        val basePath = "/data/adb/nexussu/modules/$id"
        val cmd = "touch $basePath/remove && echo 'SUCCESS'"
        return execute(cmd).contains("SUCCESS")
    }

    // NEW: Undo deferred uninstall
    fun restoreModule(id: String): Boolean {
        val basePath = "/data/adb/nexussu/modules/$id"
        val cmd = "rm -f $basePath/remove && echo 'SUCCESS'"
        return execute(cmd).contains("SUCCESS")
    }
}
