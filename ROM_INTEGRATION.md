# NexusSU: Native ROM Integration Guide

This guide is for OS maintainers and developers building custom ROMs from source. By integrating these files into your Device Tree, NexusSU will boot natively with the OS without requiring AnyKernel3 or Magisk policy injections.

## 1. Create the Init Script
In your device tree (e.g., `device/oem/codename/`), create a new directory for NexusSU if one doesn't exist, and create the initialization script.

**File:** `device/oem/codename/init/init.nexussu.rc`
```rc
# NexusSU Native Init Script
# Defines permissions for the IOCTL gateway at boot

on early-init
    chmod 0666 /dev/nexussu
    chown root root /dev/nexussu
