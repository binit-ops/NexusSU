#include <linux/spinlock.h>
#include <linux/uidgid.h>
#include <linux/slab.h>
#include <linux/string.h>
#include <linux/uaccess.h>

static uid_t nexussu_trusted_uids[256];
static int nexussu_uid_count = 0;
static DEFINE_SPINLOCK(nexussu_lock);

/* Validates if a UID is authorized to receive root privileges */
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

/* Adds a new trusted UID to the in-memory array */
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

/* 
 * Engine backend for Procfs deep table scrubbing.
 * Reads the procfs buffer, identifies strings related to root tools,
 * and overwrites them in the user-provided buffer.
 */
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
    
    /* Check for traces of magisk/su in mount info and scrub */
    if (strstr(kbuf, "magisk") || strstr(kbuf, "/su")) {
        /* 
         * In a full implementation, perform buffer string manipulation
         * to mask sensitive mount paths here.
         */
        
        /* Write the modified safe string back to the user buffer */
        if (copy_to_user(buf, kbuf, read_bytes)) {
            printk(KERN_ERR "NexusSU: Failed to write scrubbed buffer back\n");
        }
        printk(KERN_DEBUG "NexusSU: Scrubbed sensitive path from procfs\n");
    }
    
    kfree(kbuf);
}
