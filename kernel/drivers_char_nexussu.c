#include <linux/fs.h>
#include <linux/uaccess.h>
#include <linux/nexussu.h>
#include <linux/module.h>

#define NEXUSSU_IOC_MAGIC 'N'
#define NEXUSSU_ALLOW_UID _IOW(NEXUSSU_IOC_MAGIC, 1, uid_t)
#define NEXUSSU_GET_VERSION _IOR(NEXUSSU_IOC_MAGIC, 2, int)

const int ENGINE_VERSION = 100;

/* Handle incoming configuration requests from the Manager App */
static long nexussu_ioctl(struct file *file, unsigned int cmd, unsigned long arg)
{
    uid_t target_uid;
    
    switch (cmd) {
        case NEXUSSU_ALLOW_UID:
            /* Copy the UID sent by the Manager App from user space */
            if (copy_from_user(&target_uid, (uid_t __user *)arg, sizeof(uid_t))) {
                return -EFAULT;
            }
            nexussu_add_trusted_uid(target_uid);
            printk(KERN_INFO "NexusSU: Added UID %d to allowlist\n", target_uid);
            break;
            
        case NEXUSSU_GET_VERSION:
            /* Return the kernel engine version to the Manager App */
            if (copy_to_user((int __user *)arg, &ENGINE_VERSION, sizeof(int))) {
                return -EFAULT;
            }
            break;
            
        default:
            return -ENOTTY;
    }
    return 0;
}

/* Bind our custom ioctl gateway into standard file operations */
static const struct file_operations nexussu_fops = {
    .owner = THIS_MODULE,
    .unlocked_ioctl = nexussu_ioctl,
    .compat_ioctl = nexussu_ioctl,
};

static int __init nexussu_init(void) {
    /* Registration logic for the character device */
    return 0;
}
module_init(nexussu_init);
