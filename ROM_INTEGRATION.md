# NexusSU: Native ROM Integration Guide

This guide is for OS maintainers building custom ROMs from source (AOSP/LineageOS). NexusSU uses a zero-file syscall hooking architecture, meaning there is no `/dev/` node and no daemon process.

## 1. Patch the Kernel Source
NexusSU supports pre-GKI kernels (4.9, 4.14, 4.19, 5.4). 

1. Clone the NexusSU repository.
2. Navigate to the `kernel/` directory and run the patcher:
   ```bash
   chmod +x apply.sh
   ./apply.sh /path/to/your/kernel/source
   ```
   *Note: If the script fails, refer to `docs/hooks.html` for manual patching.*
3. Build your kernel as usual.

## 2. Boot Persistence (init.nexussu.rc)
Because the kernel allowlist resets on reboot, you must add an init script to re-apply grants and mount the `su` binary before Android starts.

**File:** `device/oem/codename/init/init.nexussu.rc`
```rc
# NexusSU Boot Script
on early-init
    # 1. Mount su binary
    mkdir /data/adb/nexussu/bin
    mount --bind /data/adb/nexussu/bin/su /system/bin/su

    # 2. Mount Systemless Hosts (if enabled by user)
    mount --bind /data/adb/nexussu/hosts /system/etc/hosts

    # 3. Mount all active modules (SKIP IF SAFE MODE IS ACTIVE)
    exec - root root -- /system/bin/sh -c "if [ ! -f /data/adb/nexussu/safemode ]; then find /data/adb/nexussu/modules/*/system -type f | while read file; do target=$(echo $file | sed 's|/data/adb/nexussu/modules/[^/]*/system|/system|'); mount --bind $file $target; done; fi"

    # 4. Execute post-fs-data.sh scripts for active modules
    exec - root root -- /system/bin/sh -c "if [ ! -f /data/adb/nexussu/safemode ]; then for dir in /data/adb/nexussu/modules/*; do if [ -f \$dir/post-fs-data.sh ] && [ ! -f \$dir/disable ]; then chmod 0755 \$dir/post-fs-data.sh; sh \$dir/post-fs-data.sh; fi; done; fi"

# Run the boot daemon to re-apply root grants and execute service.sh scripts
on property:sys.boot_completed=1
    exec - root root -- /data/adb/nexussu/bin/nexussu_daemon
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
