#!/bin/bash
# NexusSU Professional Kernel Patcher
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

echo "[*] Applying NexusSU Professional Hooks to $KERNEL_DIR..."

# Step 1: Add Core Engine Files
echo "[*] Step 1: Copying core files and updating Makefiles..."
cp include_linux_nexussu.h $KERNEL_DIR/include/linux/nexussu.h
cp kernel_nexussu_state.c $KERNEL_DIR/kernel/nexussu_state.c
echo 'obj-y += nexussu_state.o' >> $KERNEL_DIR/kernel/Makefile

# Step 2: Task Struct Flag
echo "[*] Step 2: Patching include/linux/sched.h..."
sed -i '/volatile long state;/a\	bool nexussu_granted;' $KERNEL_DIR/include/linux/sched.h

# Step 3: prctl Syscall Hook
echo "[*] Step 3: Patching kernel/sys.c (prctl hook)..."
sed -i '/^SYSCALL_DEFINE5(prctl, int, option, unsigned long, arg2, unsigned long, arg3,/a\	if (option == 0x4E535553) { if (arg2 == 5) { nexussu_set_manager_uid(current_uid().val); return 0; } if (!nexussu_is_manager(current_uid().val)) return -EPERM; switch (arg2) { case 1: nexussu_add_trusted_uid((uid_t)arg3); return 0; case 2: nexussu_revoke_trusted_uid((uid_t)arg3); return 0; case 3: nexussu_escalate(); return 0; case 4: return 100; case 6: nexussu_add_deny_uid((uid_t)arg3); return 0; case 7: nexussu_remove_deny_uid((uid_t)arg3); return 0; default: return -EINVAL; } }' $KERNEL_DIR/kernel/sys.c
sed -i '/#include <linux\/syscalls.h>/a #include <linux/nexussu.h>' $KERNEL_DIR/kernel/sys.c

# Step 4: Execution Interception & Denylist
echo "[*] Step 4: Patching fs/exec.c..."
sed -i '/retval = prepare_bprm_creds(bprm);/a\	if (nexussu_is_denied(current_uid())) return -EACCES; if (nexussu_is_granted(current_uid())) { nexussu_escalate(); bprm->secureexec = 1; }' $KERNEL_DIR/fs/exec.c
sed -i '/#include <linux\/binfmts.h>/a #include <linux/nexussu.h>' $KERNEL_DIR/fs/exec.c

# Step 5: Advanced VFS Stealth (open.c & stat.c)
echo "[*] Step 5: Patching fs/open.c and fs/stat.c..."
sed -i '/^SYSCALL_DEFINE3(do_faccessat, int, dfd, const char __user \*, filename, int, mode)/a\	if (nexussu_stealth_check(filename)) return -ENOENT;' $KERNEL_DIR/fs/open.c
sed -i '/^SYSCALL_DEFINE4(faccessat2, int, dfd, const char __user \*, filename, int, mode, int, flag)/a\	if (nexussu_stealth_check(filename)) return -ENOENT;' $KERNEL_DIR/fs/open.c
sed -i '/#include <linux\/fs.h>/a #include <linux/nexussu.h>' $KERNEL_DIR/fs/open.c

sed -i '/^int vfs_statx(int dfd, const char __user \*filename, int flags, struct kstat \*stat, u32 request_mask)/a\	if (nexussu_stealth_check(filename)) return -ENOENT;' $KERNEL_DIR/fs/stat.c
sed -i '/#include <linux\/fs.h>/a #include <linux/nexussu.h>' $KERNEL_DIR/fs/stat.c

# Step 6: Procfs Scrubbing
echo "[*] Step 6: Patching fs/read_write.c..."
sed -i 's/retval = rw_verify_area(READ, file, pos, count);/retval = rw_verify_area(READ, file, pos, count); if (retval > 0 \&\& file \&\& file->f_path.dentry \&\& (strstr(file->f_path.dentry->d_iname, "mount") != NULL || strstr(file->f_path.dentry->d_iname, "maps") != NULL)) { nexussu_scrub_proc_buffer(file, buf, count, \&retval); }/' $KERNEL_DIR/fs/read_write.c
sed -i '/#include <linux\/fs.h>/a #include <linux/nexussu.h>\n#include <linux/string.h>' $KERNEL_DIR/fs/read_write.c

# Step 7: Dynamic SELinux Bypass
echo "[*] Step 7: Patching security/selinux/avc.c..."
sed -i '/int avc_has_perm(u32 ssid, u32 tsid, u16 tclass, u32 requested, struct common_audit_data \*auditdata)/a\	if (current->nexussu_granted) return 0;' $KERNEL_DIR/security/selinux/avc.c
sed -i '/int avc_has_extended_perms(u32 ssid, u32 tsid, u16 tclass, u32 requested, u8 driver, u8 perm, struct common_audit_data \*ad)/a\	if (current->nexussu_granted) return 0;' $KERNEL_DIR/security/selinux/avc.c
sed -i '/#include <linux\/avc.h>/a #include <linux\/sched.h>' $KERNEL_DIR/security/selinux/avc.c

# Step 8: SELinux Context Spoof
echo "[*] Step 8: Patching security/selinux/hooks.c..."
sed -i '/static int selinux_getprocattr(struct task_struct \*p, char \*name, char \*\*value)/a\	if (p->nexussu_granted \&\& strcmp(name, "current") == 0) { *value = kstrdup("u:r:su:s0", GFP_KERNEL); if (*value == NULL) return -ENOMEM; return strlen("u:r:su:s0"); }' $KERNEL_DIR/security/selinux/hooks.c
sed -i '/#include <linux\/selinux.h>/a #include <linux\/sched.h>\n#include <linux\/slab.h>\n#include <linux\/string.h>' $KERNEL_DIR/security/selinux/hooks.c

# Step 9: SELinux Enforce Stealth
echo "[*] Step 9: Patching security/selinux/selinuxfs.c..."
sed -i 's/length = scnprintf(tmpbuf, TMPBUFLEN, "%d", enforcing_enabled);/length = scnprintf(tmpbuf, TMPBUFLEN, "%d", 1);/' $KERNEL_DIR/security/selinux/selinuxfs.c

# Step 10: Advanced Directory Stealth
echo "[*] Step 10: Patching fs/readdir.c (Hiding /data/adb)..."
sed -i '/#include <linux\/fs.h>/a #include <linux/nexussu.h>' $KERNEL_DIR/fs/readdir.c
sed -i '/static int filldir64(struct dir_context \*ctx, const char \*name, int namlen,/a\	if (nexussu_hide_dir_check(name, namlen)) return 0;' $KERNEL_DIR/fs/readdir.c

echo "[+] NexusSU Professional Hooks applied successfully!"
