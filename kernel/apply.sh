#!/bin/bash
# NexusSU Automated Kernel Patcher
# Usage: ./apply.sh /path/to/kernel/source

KERNEL_DIR=$1

if [ -z "$KERNEL_DIR" ]; then
    echo "Usage: ./apply.sh /path/to/kernel/source"
    exit 1
fi

if [ ! -d "$KERNEL_DIR/fs" ]; then
    echo "Error: $KERNEL_DIR does not appear to be a valid kernel source tree."
    exit 1
fi

echo "[*] Applying NexusSU to $KERNEL_DIR..."

# 1. Copy new standalone files
echo "[*] Copying core files..."
cp include_linux_nexussu.h $KERNEL_DIR/include/linux/nexussu.h
cp drivers_char_nexussu.c $KERNEL_DIR/drivers/char/nexussu.c
cp kernel_nexussu_state.c $KERNEL_DIR/kernel/nexussu_state.c

# 2. Update Makefiles
echo "[*] Updating Makefiles..."
echo 'obj-y += nexussu.o' >> $KERNEL_DIR/drivers/char/Makefile
echo 'obj-y += nexussu_state.o' >> $KERNEL_DIR/kernel/Makefile

# 3. Patch include/linux/sched.h (Add tracking flag)
echo "[*] Patching include/linux/sched.h..."
sed -i '/volatile long state;/a\	bool nexussu_granted;' $KERNEL_DIR/include/linux/sched.h

# 4. Patch fs/exec.c (Escalation hook)
echo "[*] Patching fs/exec.c..."
sed -i '/retval = prepare_bprm_creds(bprm);/a\	if (nexussu_is_granted(current_uid())) { nexussu_escalate(); bprm->secureexec = 1; }' $KERNEL_DIR/fs/exec.c

# 5. Patch fs/open.c & fs/stat.c (Stealth hooks)
echo "[*] Patching fs/open.c & fs/stat.c..."
sed -i '/long do_faccessat/a\	if (nexussu_stealth_check(filename)) { return -ENOENT; }' $KERNEL_DIR/fs/open.c
sed -i '/int vfs_statx/a\	if (nexussu_stealth_check(filename)) { return -ENOENT; }' $KERNEL_DIR/fs/stat.c

# 6. Patch security/selinux/avc.c (MAC Bypass)
echo "[*] Patching security/selinux/avc.c..."
sed -i '/int avc_has_perm(/a\	if (current->nexussu_granted) { return 0; }' $KERNEL_DIR/security/selinux/avc.c
sed -i '/int avc_has_extended_perms(/a\	if (current->nexussu_granted) { return 0; }' $KERNEL_DIR/security/selinux/avc.c

# 7. Patch security/selinux/hooks.c (Context Spoof)
echo "[*] Patching security/selinux/hooks.c..."
sed -i '/unsigned len;/a\	if (p->nexussu_granted \&\& strcmp(name, "current") == 0) { *value = kstrdup("u:r:su:s0", GFP_KERNEL); if (*value == NULL) return -ENOMEM; return strlen("u:r:su:s0"); }' $KERNEL_DIR/security/selinux/hooks.c

# 8. Patch security/selinux/selinuxfs.c (Enforce Stealth)
echo "[*] Patching security/selinux/selinuxfs.c..."
sed -i 's/length = scnprintf(tmpbuf, TMPBUFLEN, "%d", enforcing_enabled);/length = scnprintf(tmpbuf, TMPBUFLEN, "%d", 1);/' $KERNEL_DIR/security/selinux/selinuxfs.c

echo "[+] NexusSU applied successfully!"
echo "[!] Note: fs/readdir.c, fs/read_write.c, and includes must be added manually if required."
