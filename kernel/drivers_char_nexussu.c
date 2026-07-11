#include <linux/fs.h>
#include <linux/uaccess.h>
#include <linux/nexussu.h>
#include <linux/module.h>
#include <linux/miscdevice.h>

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

/* Define the misc device so /dev/nexussu is automatically created */
static struct miscdevice nexussu_dev = {
    .minor = MISC_DYNAMIC_MINOR,
    .name = "nexussu",           /* Creates /dev/nexussu */
    .fops = &nexussu_fops,
    .mode = 0666,                /* Allow Manager App to read/write */
};

static int __init nexussu_init(void) {
    /* Register the character device */
    int ret = misc_register(&nexussu_dev);
    if (ret) {
        printk(KERN_ERR "NexusSU: Failed to register misc device\n");
        return ret;
    }
    printk(KERN_INFO "NexusSU: Kernel engine initialized at /dev/nexussu\n");
    return 0;
}

static void __exit nexussu_exit(void) {
    misc_deregister(&nexussu_dev);
}

module_init(nexussu_init);
module_exit(nexussu_exit);
