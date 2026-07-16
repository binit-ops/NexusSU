#include <linux/spinlock.h>
#include <linux/uidgid.h>
#include <linux/slab.h>
#include <linux/string.h>
#include <linux/uaccess.h>
#include <linux/utsname.h>
#include <linux/wait.h>
#include <linux/sched.h>

static uid_t nexussu_trusted_uids[256];
static int nexussu_uid_count = 0;
static DEFINE_SPINLOCK(nexussu_lock);

static uid_t nexussu_deny_uids[256];
static int nexussu_deny_count = 0;

static uid_t nexussu_manager_uid = -1;

// NEW: Wait Queue for zero-battery-drain MagiskHide
static DECLARE_WAIT_QUEUE_HEAD(nexussu_deny_wait);
static pid_t nexussu_deny_pids[256];
static int nexussu_deny_pid_count = 0;
static DEFINE_SPINLOCK(nexussu_deny_pid_lock);

void nexussu_set_manager_uid(uid_t uid) { nexussu_manager_uid = uid; }
bool nexussu_is_manager(uid_t uid) { return (nexussu_manager_uid != -1 && uid == nexussu_manager_uid); }

bool nexussu_is_granted(kuid_t kuid) {
    int i; uid_t uid = kuid.val; unsigned long flags; bool found = false;
    spin_lock_irqsave(&nexussu_lock, flags);
    for (i = 0; i < nexussu_uid_count; i++) { if (nexussu_trusted_uids[i] == uid) { found = true; break; } }
    spin_unlock_irqrestore(&nexussu_lock, flags);
    return found;
}

void nexussu_add_trusted_uid(uid_t uid) {
    int i; unsigned long flags;
    spin_lock_irqsave(&nexussu_lock, flags);
    if (nexussu_uid_count < 256) {
        for (i = 0; i < nexussu_uid_count; i++) { if (nexussu_trusted_uids[i] == uid) { spin_unlock_irqrestore(&nexussu_lock, flags); return; } }
        nexussu_trusted_uids[nexussu_uid_count++] = uid;
    }
    spin_unlock_irqrestore(&nexussu_lock, flags);
}

void nexussu_revoke_trusted_uid(uid_t uid) {
    int i, j; unsigned long flags;
    spin_lock_irqsave(&nexussu_lock, flags);
    for (i = 0; i < nexussu_uid_count; i++) {
        if (nexussu_trusted_uids[i] == uid) {
            for (j = i; j < nexussu_uid_count - 1; j++) { nexussu_trusted_uids[j] = nexussu_trusted_uids[j+1]; }
            nexussu_uid_count--; break;
        }
    }
    spin_unlock_irqrestore(&nexussu_lock, flags);
}

// Denylist Logic
bool nexussu_is_denied(kuid_t kuid) {
    int i; uid_t uid = kuid.val; unsigned long flags; bool found = false;
    spin_lock_irqsave(&nexussu_lock, flags);
    for (i = 0; i < nexussu_deny_count; i++) { if (nexussu_deny_uids[i] == uid) { found = true; break; } }
    spin_unlock_irqrestore(&nexussu_lock, flags);
    return found;
}

void nexussu_add_deny_uid(uid_t uid) {
    int i; unsigned long flags;
    spin_lock_irqsave(&nexussu_lock, flags);
    if (nexussu_deny_count < 256) {
        for (i = 0; i < nexussu_deny_count; i++) { if (nexussu_deny_uids[i] == uid) { spin_unlock_irqrestore(&nexussu_lock, flags); return; } }
        nexussu_deny_uids[nexussu_deny_count++] = uid;
    }
    spin_unlock_irqrestore(&nexussu_lock, flags);
}

void nexussu_remove_deny_uid(uid_t uid) {
    int i, j; unsigned long flags;
    spin_lock_irqsave(&nexussu_lock, flags);
    for (i = 0; i < nexussu_deny_count; i++) {
        if (nexussu_deny_uids[i] == uid) {
            for (j = i; j < nexussu_deny_count - 1; j++) { nexussu_deny_uids[j] = nexussu_deny_uids[j+1]; }
            nexussu_deny_count--; break;
        }
    }
    spin_unlock_irqrestore(&nexussu_lock, flags);
}

// NEW: Wait Queue Logic for Daemon
void nexussu_report_deny_exec(pid_t pid) {
    spin_lock(&nexussu_deny_pid_lock);
    if (nexussu_deny_pid_count < 256) {
        nexussu_deny_pids[nexussu_deny_pid_count++] = pid;
    }
    spin_unlock(&nexussu_deny_pid_lock);
    wake_up_interruptible(&nexussu_deny_wait);
}

int nexussu_wait_for_deny_pid(void) {
    wait_event_interruptible(nexussu_deny_wait, nexussu_deny_pid_count > 0);
    
    spin_lock(&nexussu_deny_pid_lock);
    pid_t pid = -1;
    if (nexussu_deny_pid_count > 0) {
        pid = nexussu_deny_pids[--nexussu_deny_pid_count];
    }
    spin_unlock(&nexussu_deny_pid_lock);
    return pid;
}

/* VFS Stealth Check */
bool nexussu_stealth_check(const char __user *filename) {
    char buf[256];
    if (!filename) return false;
    if (copy_from_user(buf, filename, 255) == 0) {
        buf[255] = '\0';
        if (strstr(buf, "/su") != NULL || strstr(buf, "magisk") != NULL || strstr(buf, "com.nexussu.manager") != NULL || strstr(buf, "nexussu") != NULL) return true;
    }
    return false;
}

/* Directory Stealth Check */
bool nexussu_hide_dir_check(const char *name, int namlen) {
    if (!name) return false;
    if (nexussu_is_manager(current_uid().val)) return false;
    if (namlen == 7 && strncmp(name, "nexussu", 7) == 0) return true;
    if (namlen == 6 && strncmp(name, "magisk", 6) == 0) return true;
    return false;
}

/* UTSname Stealth Check */
void nexussu_scrub_utsname(struct new_utsname __user *name) {
    struct new_utsname kname;
    if (!name) return;
    if (copy_from_user(&kname, name, sizeof(kname)) == 0) {
        bool modified = false;
        char *tags[] = { "nexussu", "magisk", "lineage", "-perf", "LineageOS", "crdroid", "aosp" };
        int i;
        for (i = 0; i < 7; i++) {
            char *ptr = strstr(kname.release, tags[i]);
            if (ptr != NULL) {
                int len = strlen(tags[i]);
                memset(ptr, ' ', len);
                modified = true;
            }
        }
        if (modified) copy_to_user(name, &kname, sizeof(kname));
    }
}

/* Procfs Scrubber */
void nexussu_scrub_proc_buffer(struct file *file, char __user *buf, size_t count, ssize_t *ret) {
    char *kbuf; ssize_t read_bytes = *ret; bool modified = false; char *ptr;
    if (read_bytes <= 0 || !buf || !file) return;
    kbuf = kmalloc(read_bytes + 1, GFP_KERNEL);
    if (!kbuf) return;
    if (copy_from_user(kbuf, buf, read_bytes)) { kfree(kbuf); return; }
    kbuf[read_bytes] = '\0';
    while ((ptr = strstr(kbuf, "magisk")) != NULL) { memset(ptr, ' ', 6); modified = true; }
    while ((ptr = strstr(kbuf, "nexussu")) != NULL) { memset(ptr, ' ', 7); modified = true; }
    while ((ptr = strstr(kbuf, "/su")) != NULL) { memset(ptr, ' ', 3); modified = true; }
    if (modified) { if (copy_to_user(buf, kbuf, read_bytes)) printk(KERN_ERR "NexusSU: Failed to write scrubbed buffer back\n"); }
    kfree(kbuf);
}
