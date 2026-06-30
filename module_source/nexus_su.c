#include <linux/module.h>
#include <linux/kernel.h>

static int __init nexus_init(void) {
    printk(KERN_INFO "Nexus SU: Engine Loaded.\n");
    return 0;
}
static void __exit nexus_exit(void) {}
module_init(nexus_init);
module_exit(nexus_exit);
MODULE_LICENSE("GPL");
