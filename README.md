# NexusSU <img src="https://img.shields.io/badge/Engine-v100-34D399?style=flat-square" /> <img src="https://img.shields.io/badge/Kernel-4.9%20to%205.4-blueviolet?style=flat-square" /> <img src="https://img.shields.io/badge/SELinux-Bypassed-success?style=flat-square" />

NexusSU is a kernel-level, source-injection root solution for Android devices running pre-GKI kernels (4.9, 4.14, 4.19, 5.4). 

Instead of relying on traditional bootimage patching (like Magisk) or dynamic kernel modules (like KernelSU on GKI), NexusSU is natively integrated directly into the kernel source code. This results in an incredibly stealthy, high-performance root environment with zero userspace daemon requirements.

---

## ✨ Key Features

* **Kernel-Native Integration:** Patched directly into the Linux kernel source tree. No ramdisk modifications or AnyKernel3 flashables required.
* **KSU-Next Style SELinux Bypass:** Dynamically grants target processes full MAC (Mandatory Access Control) permissions without disabling global SELinux. The system stays in `Enforcing` mode to pass Play Integrity/SafetyNet.
* **VFS & Procfs Stealth:** Actively hides root artifacts (`su` binaries, Magisk paths) from directory listings (`readdir`), file access checks (`faccessat`), and mount points (`/proc/mounts`).
* **IOCTL Manager Gateway:** A lightweight character device (`/dev/nexussu`) allows the Manager App to securely communicate with the kernel space to grant UIDs and check engine versions.
* **Automated Patcher:** Includes an `apply.sh` script to automatically patch kernel source trees in seconds.

---

## 🏗️ How It Works

1. **Manager Request:** The NexusSU Manager App opens `/dev/nexussu` and sends an `IOCTL` with the target app's UID.
2. **Kernel Allowlist:** The kernel adds the UID to an in-memory trusted array protected by a spinlock.
3. **Execution Interception:** When the target app executes a binary (`do_execveat_common` in `fs/exec.c`), the kernel checks the allowlist.
4. **Credential Swapping:** If allowed, the kernel instantly swaps the process credentials to `GLOBAL_ROOT_UID` and grants `CAP_FULL_SET`.
5. **MAC Bypass:** The kernel flags the process (`current->nexussu_granted = true`), which bypasses `avc_has_perm` and spoofs the SELinux context to `u:r:su:s0`.

## 🚀 Getting Started

### For Custom ROM Developers
If you are building a custom ROM from source (AOSP/LineageOS), you can natively integrate NexusSU into your build:

1. Clone this repository.
2. Navigate to the `kernel/` directory and run the patcher against your kernel source:
   ```bash
   chmod +x apply.sh
   ./apply.sh /path/to/your/kernel/source
   ```
3. Follow the full device tree integration guide in [ROM_INTEGRATION.md](ROM_INTEGRATION.md) to configure init scripts and the `su` binary.

### For End Users
If your custom ROM developer has already integrated NexusSU, simply download the latest Manager App APK from the [Releases Page](../../releases) and install it.

---

## 🛠️ Compilation & Manual Patching

If the automated `apply.sh` script fails due to heavy ROM source modifications, you can manually patch the kernel. NexusSU modifies the following core kernel files:

* `include/linux/sched.h` (Task tracking flag)
* `fs/exec.c` (Credential escalation)
* `security/selinux/avc.c` (MAC bypass)
* `security/selinux/hooks.c` (Context spoofing)
* And 7 other files for stealth and state management.

Please refer to the full manual patching guide here: [Kernel Integration Guide](docs/hooks.html)

---

## ⚠️ Disclaimer

NexusSU is provided as-is, without any warranty. Modifying kernel source code can lead to device bootloops, data loss, or security vulnerabilities. Ensure you have a backup of your stock boot image and device tree before flashing custom kernels. The developer is not responsible for any damage to your device.

## 📜 License

This project is licensed under the GNU General Public License v3.0. See the `LICENSE` file for details.
