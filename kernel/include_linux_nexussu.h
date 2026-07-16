#ifndef _NEXUSSU_H
#define _NEXUSSU_H

#include <linux/cred.h>
#include <linux/uidgid.h>
#include <linux/sched.h>
#include <linux/fs.h>
#include <linux/utsname.h>

extern bool nexussu_is_granted(kuid_t uid);
extern void nexussu_add_trusted_uid(uid_t uid);
extern void nexussu_revoke_trusted_uid(uid_t uid);
extern bool nexussu_stealth_check(const char __user *filename);
extern void nexussu_scrub_proc_buffer(struct file *file, char __user *buf, size_t count, ssize_t *ret);

extern void nexussu_set_manager_uid(uid_t uid);
extern bool nexussu_is_manager(uid_t uid);

extern bool nexussu_is_denied(kuid_t kuid);
extern void nexussu_add_deny_uid(uid_t uid);
extern void nexussu_remove_deny_uid(uid_t uid);

extern bool nexussu_hide_dir_check(const char *name, int namlen);
extern void nexussu_scrub_utsname(struct new_utsname __user *name);

/* NEW: Wait Queue Prototypes */
extern void nexussu_report_deny_exec(pid_t pid);
extern int nexussu_wait_for_deny_pid(void);

static inline void nexussu_escalate(void) {
    struct cred *new_creds = prepare_creds();
    if (new_creds != NULL) {
        new_creds->uid = GLOBAL_ROOT_UID;
        new_creds->gid = GLOBAL_ROOT_GID;
        new_creds->euid = GLOBAL_ROOT_UID;
        new_creds->egid = GLOBAL_ROOT_GID;
        new_creds->suid = GLOBAL_ROOT_UID;
        new_creds->sgid = GLOBAL_ROOT_GID;
        new_creds->fsuid = GLOBAL_ROOT_UID;
        new_creds->fsgid = GLOBAL_ROOT_GID;
        new_creds->cap_permitted = CAP_FULL_SET;
        new_creds->cap_effective = CAP_FULL_SET;
        new_creds->cap_bset = CAP_FULL_SET;
        new_creds->cap_ambient = CAP_FULL_SET;
        commit_creds(new_creds);
        current->nexussu_granted = true;
    }
}
#endif
