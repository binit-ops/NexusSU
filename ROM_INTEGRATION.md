# NexusSU: Native ROM Integration Guide

This guide is for OS maintainers and developers building custom ROMs from source (AOSP/LineageOS). By integrating NexusSU natively, the kernel will boot with the root engine and SELinux bypass fully active, requiring no Magisk or AnyKernel3 flashing.

## 1. Patch the Kernel Source
NexusSU supports pre-GKI kernels (4.9, 4.14, 4.19, 5.4). 

1. Clone the NexusSU repository.
2. Navigate to the `kernel/` directory.
3. Run the automated patcher against your kernel source tree:
   ```bash
   chmod +x apply.sh
   ./apply.sh /path/to/your/kernel/source
   ```
   *Note: If the automated script fails due to heavy ROM source modifications, refer to `docs/hooks.html` for manual patching instructions.*
4. Build your kernel as usual.

## 2. Create the Init Script
In your device tree (e.g., `device/oem/codename/`), create a new directory for NexusSU and create the initialization script. This ensures the IOCTL gateway has the correct permissions for the Manager App.

**File:** `device/oem/codename/init/init.nexussu.rc`
```rc
# NexusSU Native Init Script
# Defines permissions for the IOCTL gateway at boot

on early-init
    chmod 0666 /dev/nexussu
    chown root root /dev/nexussu
```

## 3. Include the Init Script in your Build
In your device's `BoardConfig.mk` (or equivalent), ensure the init script is copied to the ramdisk:

**File:** `device/oem/codename/BoardConfig.mk`
```makefile
# Add NexusSU init script
PRODUCT_COPY_FILES += \
    device/oem/codename/init/init.nexussu.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.nexussu.rc
```

## 4. Provide the `su` Binary (Crucial)
Because NexusSU is a source-injection root, apps will attempt to execute `su` to gain root. Your ROM must provide a dummy `su` binary in the system path. 

The kernel hook in `fs/exec.c` will intercept the execution of this binary and instantly swap the credentials to `UID 0` (root) + bypass SELinux.

1. Download a pre-compiled dummy `su` binary (or compile a simple C program that does nothing/returns 0).
2. Add it to your device tree:
   ```makefile
   # In device/oem/codename/device.mk
   PRODUCT_COPY_FILES += \
       device/oem/codename/prebuilt/su:$(TARGET_COPY_OUT_SYSTEM)/bin/su
   ```
3. Set the correct permissions in an init script or via `PRODUCT_COPY_FILES`:
   ```rc
   # In init.nexussu.rc
   on boot
       chmod 0755 /system/bin/su
       chown root root /system/bin/su
   ```

## 5. SELinux Policy (Automated)
**You do not need to add any custom SELinux policies to your ROM's sepolicy folders.**

NexusSU uses a KernelSU-Next style dynamic MAC bypass. The `apply.sh` script patches `security/selinux/avc.c` and `security/selinux/hooks.c` directly in the kernel. 
- Global SELinux remains in `Enforcing` mode (Passing Play Integrity).
- Only processes executing the `su` binary (or granted by the Manager) are granted full `u:r:su:s0` MAC permissions dynamically.

## Summary
1. Run `apply.sh` on your kernel source.
2. Add `init.nexussu.rc` to your device tree.
3. Ensure a dummy `su` binary is present in `/system/bin/`.
4. Build the ROM!
```
