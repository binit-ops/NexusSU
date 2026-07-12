#include <linux/spinlock.h>
#include <linux/uidgid.h>
#include <linux/slab.h>
#include <linux/string.h>
#include <linux/uaccess.h>

static uid_t nexussu_trusted_uids[256];
static int nexussu_uid_count = 0;
static DEFINE_SPINLOCK(nexussu_lock);

bool nexussu_is_granted(kuid_t kuid) {
    int i;
    uid_t uid = kuid.val;
    unsigned long flags;
    bool found = false;
    
    spin_lock_irqsave(&nexussu_lock, flags);
    for (i = 0; i < nexussu_uid_count; i++) {
        if (nexussu_trusted_uids[i] == uid) {
            found = true;
            break;
        }
    }
    spin_unlock_irqrestore(&nexussu_lock, flags);
    return found;
}

void nexussu_add_trusted_uid(uid_t uid) {
    int i;
    unsigned long flags;
    
    spin_lock_irqsave(&nexussu_lock, flags);
    if (nexussu_uid_count < 256) {
        for (i = 0; i < nexussu_uid_count; i++) {
            if (nexussu_trusted_uids[i] == uid) {
                spin_unlock_irqrestore(&nexussu_lock, flags);
                return;
            }
        }
        nexussu_trusted_uids[nexussu_uid_count++] = uid;
    }
    spin_unlock_irqrestore(&nexussu_lock, flags);
}

// NEW: Real revocation logic
void nexussu_revoke_trusted_uid(uid_t uid) {
    int i, j;
    unsigned long flags;
    
    spin_lock_irqsave(&nexussu_lock, flags);
    for (i = 0; i < nexussu_uid_count; i++) {
        if (nexussu_trusted_uids[i] == uid) {
            // Shift array down to overwrite the revoked UID
            for (j = i; j < nexussu_uid_count - 1; j++) {
                nexussu_trusted_uids[j] = nexussu_trusted_uids[j+1];
            }
            nexussu_uid_count--;
            break;
        }
    }
    spin_unlock_irqrestore(&nexussu_lock, flags);
}

void nexussu_scrub_proc_buffer(struct file *file, char __user *buf, size_t count, ssize_t *ret) {
    char *kbuf;
    ssize_t read_bytes = *ret;
    
    if (read_bytes <= 0 || !buf || !file) return;
    
    kbuf = kmalloc(read_bytes + 1, GFP_KERNEL);
    if (!kbuf) return;
    
    if (copy_from_user(kbuf, buf, read_bytes)) { 
        kfree(kbuf); 
        return; 
    }
    kbuf[read_bytes] = '\0';
    
    if (strstr(kbuf, "magisk") || strstr(kbuf, "/su")) {
        if (copy_to_user(buf, kbuf, read_bytes)) {
            printk(KERN_ERR "NexusSU: Failed to write scrubbed buffer back\n");
        }
        printk(KERN_DEBUG "NexusSU: Scrubbed sensitive path from procfs\n");
    }
    
    kfree(kbuf);
}
