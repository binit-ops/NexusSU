/* Minimal safe susfs stub - logs requests only */
#include <linux/module.h>
#include <linux/proc_fs.h>
#include <linux/uaccess.h>
#define PROC_NAME "susfs"
#define MAX_BUF 256
static struct proc_dir_entry *susfs_entry;
static ssize_t susfs_write(struct file *file, const char __user *buf, size_t count, loff_t *ppos) {
  char kbuf[MAX_BUF];
  if (count >= MAX_BUF) return -EINVAL;
  if (copy_from_user(kbuf, buf, count)) return -EFAULT;
  kbuf[count] = '\0';
  if (strncmp(kbuf, "REQ:", 4) != 0) {
    pr_warn("susfs: invalid request format\n");
    return -EINVAL;
  }
  pr_info("susfs: received request: %s\n", kbuf);
  return count;
}
static const struct proc_ops susfs_fops = { .proc_write = susfs_write, };
static int __init susfs_init(void) {
  susfs_entry = proc_create(PROC_NAME, 0600, NULL, &susfs_fops);
  if (!susfs_entry) { pr_err("susfs: failed to create proc entry\n"); return -ENOMEM; }
  pr_info("susfs: stub initialized\n");
  return 0;
}
static void __exit susfs_exit(void) { proc_remove(susfs_entry); pr_info("susfs: stub removed\n"); }
module_init(susfs_init); module_exit(susfs_exit);
MODULE_LICENSE("GPL"); MODULE_AUTHOR("NexusSU"); MODULE_DESCRIPTION("Safe susfs stub");
  
