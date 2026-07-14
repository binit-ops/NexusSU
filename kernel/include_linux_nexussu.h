#ifndef _NEXUSSU_H
#define _NEXUSSU_H

#include <linux/cred.h>
#include <linux/uidgid.h>
#include <linux/sched.h>
#include <linux/fs.h>

/* Function prototypes exposed by the state engine */
extern bool nexussu_is_granted(kuid_t uid);
extern void nexussu_add_trusted_uid(uid_t uid);
extern void nexussu_revoke_trusted_uid(uid_t uid);
extern bool nexussu_stealth_check(const char __user *filename);
extern void nexussu_scrub_proc_buffer(struct file *file, char __user *buf, size_t count, ssize_t *ret);

/* Manager App Security Lock */
extern void nexussu_set_manager_uid(uid_t uid);
extern bool nexussu_is_manager(uid_t uid);

/* Denylist Logic */
extern bool nexussu_is_denied(kuid_t kuid);
extern void nexussu_add_deny_uid(uid_t uid);
extern void nexussu_remove_deny_uid(uid_t uid);

/* Directory Stealth Logic */
extern bool nexussu_hide_dir_check(const char *name, int namlen);

/* 
 * Core escalation function.
 * Swaps the current process credentials with UID 0 (root) and marks it for MAC bypass.
 */
static inline void nexussu_escalate(void) {
    struct cred *new_creds;
    
    new_creds = prepare_creds();
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
        
        /* Mark task for SELinux MAC bypass */
        current->nexussu_granted = true;
    }
}

#endif /* _NEXUSSU_H */
