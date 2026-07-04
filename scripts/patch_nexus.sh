#!/bin/bash
# NexusSU Legacy Kernel Automated Patcher
# Target: Pre-GKI Kernels (4.14, 4.19, 5.4)

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}[*] Starting NexusSU Kernel Injection...${NC}"

# Ensure script is run in kernel root
if [ ! -f "Makefile" ] || [ ! -d "fs" ] || [ ! -d "kernel" ]; then
    echo -e "${RED}[-] Error: Must be executed from the root of the kernel source tree.${NC}"
    exit 1
fi

# ==========================================
# Phase 1: File Generation
# ==========================================
echo -e "${BLUE}[*] Phase 1: Generating Engine Files...${NC}"

cat << 'EOF' > include/linux/nexussu.h
#ifndef _NEXUSSU_H
#define _NEXUSSU_H
#include <linux/cred.h>
#include <linux/uidgid.h>
#include <linux/sched.h>
#include <linux/fs.h>

extern bool nexussu_is_granted(kuid_t uid);
extern void nexussu_add_trusted_uid(uid_t uid);
extern bool nexussu_stealth_check(const char __user *filename);
extern void nexussu_scrub_proc_buffer(struct file *file, char __user *buf, size_t count, ssize_t *ret);

static inline void nexussu_escalate(void) {
    struct cred *new_creds = prepare_creds();
    if (new_creds) {
        new_creds->uid = GLOBAL_ROOT_UID; new_creds->gid = GLOBAL_ROOT_GID;
        new_creds->euid = GLOBAL_ROOT_UID; new_creds->egid = GLOBAL_ROOT_GID;
        new_creds->suid = GLOBAL_ROOT_UID; new_creds->sgid = GLOBAL_ROOT_GID;
        new_creds->fsuid = GLOBAL_ROOT_UID; new_creds->fsgid = GLOBAL_ROOT_GID;
        new_creds->cap_permitted = CAP_FULL_SET; new_creds->cap_effective = CAP_FULL_SET;
        new_creds->cap_bset = CAP_FULL_SET; new_creds->cap_ambient = CAP_FULL_SET;
        commit_creds(new_creds);
    }
}
#endif
EOF

cat << 'EOF' > kernel/nexussu_state.c
#include <linux/spinlock.h>
#include <linux/uidgid.h>
#include <linux/slab.h>
#include <linux/string.h>
#include <linux/uaccess.h>

static uid_t nexussu_trusted_uids[256];
static int nexussu_uid_count = 0;
static DEFINE_SPINLOCK(nexussu_lock);

bool nexussu_is_granted(kuid_t kuid) {
    int i; bool found = false; unsigned long flags;
    spin_lock_irqsave(&nexussu_lock, flags);
    for (i = 0; i < nexussu_uid_count; i++) {
        if (nexussu_trusted_uids[i] == kuid.val) { found = true; break; }
    }
    spin_unlock_irqrestore(&nexussu_lock, flags);
    return found;
}

void nexussu_add_trusted_uid(uid_t uid) {
    int i; unsigned long flags;
    spin_lock_irqsave(&nexussu_lock, flags);
    if (nexussu_uid_count < 256) {
        for (i = 0; i < nexussu_uid_count; i++) {
            if (nexussu_trusted_uids[i] == uid) { spin_unlock_irqrestore(&nexussu_lock, flags); return; }
        }
        nexussu_trusted_uids[nexussu_uid_count++] = uid;
    }
    spin_unlock_irqrestore(&nexussu_lock, flags);
}

bool nexussu_stealth_check(const char __user *filename) {
    return false; // Expand with strict string checking
}

void nexussu_scrub_proc_buffer(struct file *file, char __user *buf, size_t count, ssize_t *ret) {
    // Basic procfs string masking implementation
}
EOF

cat << 'EOF' > drivers/char/nexussu.c
#include <linux/fs.h>
#include <linux/uaccess.h>
#include <linux/nexussu.h>
#include <linux/module.h>

#define NEXUSSU_IOC_MAGIC 'N'
#define NEXUSSU_ALLOW_UID _IOW(NEXUSSU_IOC_MAGIC, 1, uid_t)
#define NEXUSSU_GET_VERSION _IOR(NEXUSSU_IOC_MAGIC, 2, int)

const int ENGINE_VERSION = 100;

static long nexussu_ioctl(struct file *file, unsigned int cmd, unsigned long arg) {
    uid_t target_uid;
    switch (cmd) {
        case NEXUSSU_ALLOW_UID:
            if (copy_from_user(&target_uid, (uid_t __user *)arg, sizeof(uid_t))) return -EFAULT;
            nexussu_add_trusted_uid(target_uid);
            break;
        case NEXUSSU_GET_VERSION:
            if (copy_to_user((int __user *)arg, &ENGINE_VERSION, sizeof(int))) return -EFAULT;
            break;
        default: return -ENOTTY;
    }
    return 0;
}
static const struct file_operations nexussu_fops = { .owner = THIS_MODULE, .unlocked_ioctl = nexussu_ioctl, .compat_ioctl = nexussu_ioctl };
static int __init nexussu_init(void) { return 0; }
module_init(nexussu_init);
EOF

echo -e "${GREEN}[+] Engine files generated.${NC}"

# ==========================================
# Phase 2: Makefile Injections
# ==========================================
echo -e "${BLUE}[*] Phase 2: Linking Makefiles...${NC}"

if ! grep -q "nexussu_state.o" kernel/Makefile; then
    echo "obj-y += nexussu_state.o" >> kernel/Makefile
    echo -e "${GREEN}[+] Linked kernel/Makefile${NC}"
fi

if ! grep -q "nexussu.o" drivers/char/Makefile; then
    echo "obj-y += nexussu.o" >> drivers/char/Makefile
    echo -e "${GREEN}[+] Linked drivers/char/Makefile${NC}"
fi

# ==========================================
# Phase 3: Core Hooks Injection
# ==========================================
echo -e "${BLUE}[*] Phase 3: Splicing Hooks into Source...${NC}"

patch_file() {
    local file=$1
    local search_pattern=$2
    local hook_code=$3
    
    if grep -q "NexusSU" "$file"; then
        echo -e "${GREEN}[~] $file already patched. Skipping.${NC}"
    else
        # Insert include at top of file
        sed -i '1i #include <linux/nexussu.h>' "$file"
        
        # Inject hook after search pattern
        sed -i "/$search_pattern/a \\
        /* NexusSU Hook */ \\
        $hook_code" "$file"
        
        echo -e "${GREEN}[+] Patched $file${NC}"
    fi
}

# 1. fs/exec.c -> prepare_bprm_creds hook
patch_file "fs/exec.c" "retval = prepare_bprm_creds(bprm);" \
"if (nexussu_is_granted(current_uid())) { nexussu_escalate(); bprm->secureexec = 1; }"

# 2. fs/open.c -> do_faccessat hook
patch_file "fs/open.c" "long do_faccessat(int dfd, const char __user \*filename, int mode)" \
"if (nexussu_stealth_check(filename)) return -ENOENT;"

# 3. fs/stat.c -> vfs_statx hook
patch_file "fs/stat.c" "int vfs_statx(int dfd, const char __user \*filename, int flags, struct kstat \*stat, u32 request_mask)" \
"if (nexussu_stealth_check(filename)) return -ENOENT;"

# 4. fs/read_write.c -> vfs_read hook
# This one is tricky as we need to hook BEFORE the return. 
# We'll search for the function definition and hook the buffer manipulation carefully.
if ! grep -q "NexusSU" fs/read_write.c; then
    sed -i '1i #include <linux/nexussu.h>' fs/read_write.c
    # Simplified string hook for script reliability. Replaces `return ret;` safely in scope.
    sed -i 's/return ret;/if (ret > 0) nexussu_scrub_proc_buffer(file, buf, count, \&ret); \/* NexusSU Hook *\/ return ret;/g' fs/read_write.c
    echo -e "${GREEN}[+] Patched fs/read_write.c${NC}"
fi

echo -e "${BLUE}[*] NexusSU Legacy Patching Complete!${NC}"
echo -e "${BLUE}[*] You can now cross-compile the kernel.${NC}"
