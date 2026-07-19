# NexusSU: Native ROM Integration Guide

This guide is for OS maintainers building custom ROMs from source (AOSP/LineageOS). NexusSU uses a zero-file syscall hooking architecture, meaning there is no `/dev/` node and no daemon process running in the background.

## 1. Patch the Kernel Source
NexusSU supports pre-GKI kernels (4.9, 4.14, 4.19, 5.4). 

1. Clone the NexusSU repository.
2. Navigate to the `kernel/` directory and run the patcher:
   ```bash
   chmod +x apply.sh
   ./apply.sh /path/to/your/kernel/source
   ```
   *Note: If the script fails due to heavy ROM source modifications, refer to the [Manual Patching Guide](https://binit-ops.github.io/NexusSU/hooks.html) for step-by-step instructions.*
3. Build your kernel as usual.

## 2. Boot Persistence (init.nexussu.rc)
Because the kernel allowlist resets on reboot, you must add an init script to re-apply grants and mount the `su` binary before Android starts. Modern Android (10+) uses File-Based Encryption (FBE), so `/data` is not readable during `early-init`. The script must run at `post-fs-data`.

**File:** `device/oem/codename/init/init.nexussu.rc`
```rc
# NexusSU Boot Script

# Phase 1: post-fs-data (Data partition is decrypted here)
on post-fs-data
    # Skip everything if Safe Mode is active
    if [ -f /data/adb/nexussu/safemode ]; then return 0; fi

    # 1. Mount su binary to all standard paths
    mkdir /data/adb/nexussu/bin
    mount --bind /data/adb/nexussu/bin/su /system/bin/su
    mkdir /system/xbin
    mount --bind /data/adb/nexussu/bin/su /system/xbin/su
    mkdir /system/sbin
    mount --bind /data/adb/nexussu/bin/su /system/sbin/su

    # 2. Mount Systemless Hosts (if enabled by user)
    mount --bind /data/adb/nexussu/hosts /system/etc/hosts

    # 3. Mount all active modules (SKIP IF skip_mount EXISTS)
    exec - root root -- /system/bin/sh -c "for dir in /data/adb/nexussu/modules/*; do if [ -f \$dir/disable ] || [ -f \$dir/skip_mount ]; then continue; fi; if [ -d \$dir/system ]; then find \$dir/system -type f | while read file; do target=$(echo \$file | sed 's|\$dir/system|/system|'); mount --bind \$file \$target; done; fi; done"

    # 4. Execute post-fs-data.sh scripts for active modules
    exec - root root -- /system/bin/sh -c "for dir in /data/adb/nexussu/modules/*; do if [ -f \$dir/disable ]; then continue; fi; if [ -f \$dir/post-fs-data.sh ]; then chmod 0755 \$dir/post-fs-data.sh; sh \$dir/post-fs-data.sh; fi; done"

# Phase 2: boot (Zygote has started, safe to run background loops)
service nexussu_daemon /data/adb/nexussu/bin/nexussu_daemon
    user root
    group root
    seclabel u:r:su:s0
    oneshot
```

## 3. Include the Init Script in your Build
In your device's `BoardConfig.mk`, ensure the init script is copied to the ramdisk:

```makefile
PRODUCT_COPY_FILES += \
    device/oem/codename/init/init.nexussu.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.nexussu.rc
```

## 4. SELinux Policy (Automated)
**You do not need to add any custom SELinux policies.**
NexusSU uses a dynamic MAC bypass. The `apply.sh` script patches `security/selinux/avc.c` and `security/selinux/hooks.c` directly in the kernel. Global SELinux remains in `Enforcing` mode to pass Play Integrity.
