#!/bin/bash
# NexusSU Professional Kernel Patcher

KERNEL_DIR=$1
if [ -z "$KERNEL_DIR" ]; then echo "Usage: ./apply.sh /path/to/kernel/source"; exit 1; fi
if [ ! -d "$KERNEL_DIR/fs" ]; then echo "Error: Invalid kernel tree."; exit 1; fi

echo "[*] Applying NexusSU Professional Hooks to $KERNEL_DIR..."

# Step 1: Add Core Engine Files
echo "[*] Step 1: Copying core files..."
cp include_linux_nexussu.h $KERNEL_DIR/include/linux/nexussu.h
cp kernel_nexussu_state.c $KERNEL_DIR/kernel/nexussu_state.c
echo 'obj-y += nexussu_state.o' >> $KERNEL_DIR/kernel/Makefile

# Step 2: Non-Intrusive Task Tracking (Zero KABI Breakage)
echo "[*] Step 2: Patching kernel/exit.c (Process cleanup)..."
sed -i '/void do_exit(long code)/a\	nexussu_set_granted(current->pid, false);' $KERNEL_DIR/kernel/exit.c
sed -i '/#include <linux\/sched.h>/a #include <linux/nexussu.h>' $KERNEL_DIR/kernel/exit.c

# Step 3: prctl Syscall Hook & Namespace Safety
echo "[*] Step 3: Patching kernel/sys.c (prctl hook)..."
sed -i '/^SYSCALL_DEFINE5(prctl, int, option, unsigned long, arg2, unsigned long, arg3,/a\	if (option == 0x4E535553) { uid_t caller_uid = from_kuid(&init_user_ns, current_uid()); if (!uid_eq(current_uid(), GLOBAL_ROOT_UID)) { if (arg2 == 5) { if (!nexussu_check_manager()) { nexussu_set_manager_uid(caller_uid); return 0; } return -EPERM; } if (!nexussu_is_manager(caller_uid)) return -EPERM; } switch (arg2) { case 1: nexussu_add_trusted_uid((uid_t)arg3); return 0; case 2: nexussu_revoke_trusted_uid((uid_t)arg3); return 0; case 3: nexussu_escalate(); return 0; case 4: return 100; case 6: nexussu_add_deny_uid((uid_t)arg3); return 0; case 7: nexussu_remove_deny_uid((uid_t)arg3); return 0; case 8: return nexussu_wait_for_deny_pid(); case 9: return nexussu_check_manager(); case 10: nexussu_reset_manager(); return 0; default: return -EINVAL; } }' $KERNEL_DIR/kernel/sys.c
sed -i '/#include <linux\/syscalls.h>/a #include <linux/nexussu.h>' $KERNEL_DIR/kernel/sys.c

# Step 4: Execution Interception & Denylist Reporting
echo "[*] Step 4: Patching fs/exec.c..."
sed -i '/retval = prepare_bprm_creds(bprm);/a\	if (nexussu_is_denied(current_uid())) { if (nexussu_stealth_check(bprm->filename)) return -EACCES; nexussu_report_deny_exec(current->pid); } if (nexussu_is_granted(current->pid)) { nexussu_escalate(); }' $KERNEL_DIR/fs/exec.c
sed -i '/#include <linux\/binfmts.h>/a #include <linux/nexussu.h>' $KERNEL_DIR/fs/exec.c

# Step 5: Low-Level VFS Stealth Resolution
echo "[*] Step 5: Patching fs/namei.c and fs/stat.c..."
sed -i '/static struct dentry \*filename_lookup(int dfd, struct filename \*name, unsigned flags, struct path \*path, struct path \*root)/a\	if (name && nexussu_stealth_check(name->name)) return -ENOENT;' $KERNEL_DIR/fs/namei.c
sed -i '/#include <linux\/namei.h>/a #include <linux/nexussu.h>' $KERNEL_DIR/fs/namei.c
sed -i '/int vfs_getattr_nosec(const struct path \*path, struct kstat \*stat, u32 request_mask, unsigned int query_flags)/a\	if (path && path->dentry && nexussu_stealth_check(path->dentry->d_name.name)) return -ENOENT;' $KERNEL_DIR/fs/stat.c
sed -i '/#include <linux\/fs.h>/a #include <linux/nexussu.h>' $KERNEL_DIR/fs/stat.c

# Step 6: Targeted Procfs Filtering (Zero I/O Penalty)
echo "[*] Step 6: Patching fs/namespace.c and fs/proc/task_mmu.c..."
sed -i '/static int show_mountinfo(struct seq_file \*m, struct vfsmount \*mnt)/a\	if (mnt && mnt->mnt_devname && nexussu_stealth_check(mnt->mnt_devname)) return 0;' $KERNEL_DIR/fs/namespace.c
sed -i '/#include <linux\/mount.h>/a #include <linux/nexussu.h>' $KERNEL_DIR/fs/namespace.c
sed -i '/static int m_show(struct seq_file \*m, void \*v)/a\	if (vma && vma->vm_file && nexussu_stealth_check(vma->vm_file->f_path.dentry->d_name.name)) return 0;' $KERNEL_DIR/fs/proc/task_mmu.c
sed -i '/#include <linux\/mm.h>/a #include <linux/nexussu.h>' $KERNEL_DIR/fs/proc/task_mmu.c

# Step 7: Dynamic SELinux Bypass
echo "[*] Step 7: Patching security/selinux/avc.c..."
sed -i '/int avc_has_perm(u32 ssid, u32 tsid, u16 tclass, u32 requested, struct common_audit_data \*auditdata)/a\	if (nexussu_is_granted(current->pid)) return 0;' $KERNEL_DIR/security/selinux/avc.c
sed -i '/int avc_has_extended_perms(u32 ssid, u32 tsid, u16 tclass, u32 requested, u8 driver, u8 perm, struct common_audit_data \*ad)/a\	if (nexussu_is_granted(current->pid)) return 0;' $KERNEL_DIR/security/selinux/avc.c
sed -i '/#include <linux\/avc.h>/a #include <linux/nexussu.h>' $KERNEL_DIR/security/selinux/avc.c

# Step 8: SELinux Context Spoof
echo "[*] Step 8: Patching security/selinux/hooks.c..."
sed -i '/static int selinux_getprocattr(struct task_struct \*p, char \*name, char \*\*value)/a\	if (nexussu_is_granted(p->pid) && strcmp(name, "current") == 0) { *value = kstrdup("u:r:su:s0", GFP_KERNEL); if (*value == NULL) return -ENOMEM; return strlen("u:r:su:s0"); }' $KERNEL_DIR/security/selinux/hooks.c
sed -i '/#include <linux\/selinux.h>/a #include <linux/nexussu.h>\n#include <linux\/slab.h>\n#include <linux\/string.h>' $KERNEL_DIR/security/selinux/hooks.c

# Step 9: SELinux Enforce Stealth
echo "[*] Step 9: Patching security/selinux/selinuxfs.c..."
sed -i 's/length = scnprintf(tmpbuf, TMPBUFLEN, "%d", enforcing_enabled);/length = scnprintf(tmpbuf, TMPBUFLEN, "%d", 1);/' $KERNEL_DIR/security/selinux/selinuxfs.c

# Step 10: Advanced Directory Stealth
echo "[*] Step 10: Patching fs/readdir.c..."
sed -i '/#include <linux\/fs.h>/a #include <linux/nexussu.h>' $KERNEL_DIR/fs/readdir.c
sed -i '/static int filldir64(struct dir_context \*ctx, const char \*name, int namlen,/a\	if (nexussu_hide_dir_check(name, namlen)) return 0;' $KERNEL_DIR/fs/readdir.c

# Step 11: UTSname Stealth
echo "[*] Step 11: Patching kernel/sys.c (uname scrubbing)..."
sed -i '/SYSCALL_DEFINE1(newuname, struct new_utsname __user \*, name)/,/^}/ s/return errno;/if (errno == 0) { nexussu_scrub_utsname(name); } return errno;/' $KERNEL_DIR/kernel/sys.c

# Step 12: SELinux Write Protection
echo "[*] Step 12: Patching security/selinux/selinuxfs.c (Block SELinux disabling)..."
sed -i '/static ssize_t sel_write_enforce(struct file \*file, const char __user \*buf,/a\	uid_t caller_uid = from_kuid(&init_user_ns, current_uid()); if (!nexussu_is_manager(caller_uid)) return -EACCES;' $KERNEL_DIR/security/selinux/selinuxfs.c

echo "[+] NexusSU Professional Hooks applied successfully!"
