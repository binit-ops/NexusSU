#include <linux/module.h>
#include <linux/kprobes.h>
#include <linux/fs.h>
#include <linux/cred.h>
#include <linux/miscdevice.h>
#include <linux/uaccess.h>
#include <linux/kthread.h>
#include <linux/delay.h>
#include <linux/umh.h>
#include <linux/namei.h>

#define NEXUSSU_IOCTL_ELEVATE _IO('n', 1)
#define NEXUSSU_IOCTL_CHECK   _IO('n', 2)

static pid_t elevated_pid = 0;
static struct kretprobe my_selinux_kretprobe;

static int nexussu_selinux_ret(struct kretprobe_instance *ri, struct pt_regs *regs) {
    if (current->pid == elevated_pid && elevated_pid != 0) { regs->regs[0] = 0; }
    return 0;
}

static int nexussu_trigger_daemon(void *data) {
    char *argv[] = { "/data/adb/nexussud", NULL };
    char *envp[] = { "HOME=/", "PATH=/system/bin:/system/xbin", NULL };
    struct path path;
    int attempts = 0;
    while (attempts < 60) {
        if (kern_path(argv[0], LOOKUP_FOLLOW, &path) == 0) {
            path_put(&path);
            call_usermodehelper(argv[0], argv, envp, UMH_WAIT_PROC);
            break;
        }
        msleep(1000); attempts++;
    }
    return 0;
}

static long nexussu_ioctl(struct file *file, unsigned int cmd, unsigned long arg) {
    if (cmd == NEXUSSU_IOCTL_CHECK) return 0x4E585355;
    if (cmd == NEXUSSU_IOCTL_ELEVATE) {
        struct cred *new_cred = prepare_creds();
        new_cred->uid.val = 0; new_cred->gid.val = 0;
        commit_creds(new_cred);
        elevated_pid = current->pid;
        return 0;
    }
    return -EINVAL;
}

static const struct file_operations nexussu_fops = { .unlocked_ioctl = nexussu_ioctl };
static struct miscdevice nexussu_misc = { .minor = MISC_DYNAMIC_MINOR, .name = "nexussu", .fops = &nexussu_fops };

static int __init nexussu_init(void) {
    misc_register(&nexussu_misc);
    my_selinux_kretprobe.kp.symbol_name = "selinux_capable";
    my_selinux_kretprobe.handler = nexussu_selinux_ret;
    register_kretprobe(&my_selinux_kretprobe);
    kthread_run(nexussu_trigger_daemon, NULL, "nexussu_init");
    return 0;
}

static void __exit nexussu_exit(void) { unregister_kretprobe(&my_selinux_kretprobe); misc_deregister(&nexussu_misc); }
module_init(nexussu_init); module_exit(nexussu_exit); MODULE_LICENSE("GPL");
