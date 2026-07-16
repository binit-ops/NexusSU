#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include <sys/prctl.h>
#include <sys/stat.h>
#include <unistd.h>

#define NEXUSSU_PRCTL_MAGIC 0x4E535553
#define CMD_REGISTER_MANAGER 5
#define CMD_GRANT_UID 1
#define CMD_ESCALATE_SELF 3

#define MODULES_DIR "/data/adb/nexussu/modules"
#define DENYLIST_PATH "/data/adb/nexussu/denylist.txt"

void apply_saved_root_grants() {
    FILE *file = fopen("/data/adb/nexussu/granted_uids.txt", "r");
    if (!file) return;

    char line[32];
    prctl(NEXUSSU_PRCTL_MAGIC, CMD_REGISTER_MANAGER, 0, 0, 0);

    while (fgets(line, sizeof(line), file)) {
        int uid = atoi(line);
        if (uid > 0) {
            prctl(NEXUSSU_PRCTL_MAGIC, CMD_GRANT_UID, (unsigned long)uid, 0, 0);
        }
    }
    fclose(file);
}

void execute_module_scripts() {
    DIR *dir = opendir(MODULES_DIR);
    if (!dir) return;

    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_name[0] == '.') continue;

        char module_path[512];
        snprintf(module_path, sizeof(module_path), "%s/%s", MODULES_DIR, entry->d_name);

        char disable_path[600];
        snprintf(disable_path, sizeof(disable_path), "%s/disable", module_path);
        if (access(disable_path, F_OK) == 0) continue;

        char script_path[600];
        snprintf(script_path, sizeof(script_path), "%s/service.sh", module_path);
        if (access(script_path, F_OK) == 0) {
            chmod(script_path, 0755);
            char cmd[700];
            snprintf(cmd, sizeof(cmd), "sh %s >/dev/null 2>&1 &", script_path);
            system(cmd);
        }
    }
    closedir(dir);
}

void apply_module_props() {
    DIR *dir = opendir(MODULES_DIR);
    if (!dir) return;

    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_name[0] == '.') continue;

        char module_path[512];
        snprintf(module_path, sizeof(module_path), "%s/%s", MODULES_DIR, entry->d_name);

        char disable_path[600];
        snprintf(disable_path, sizeof(disable_path), "%s/disable", module_path);
        if (access(disable_path, F_OK) == 0) continue;

        char prop_path[600];
        snprintf(prop_path, sizeof(prop_path), "%s/system.prop", module_path);
        
        if (access(prop_path, F_OK) == 0) {
            FILE *prop_file = fopen(prop_path, "r");
            if (!prop_file) continue;
            
            char line[512];
            while (fgets(line, sizeof(line), prop_file)) {
                if (line[0] == '#' || line[0] == '\n') continue;
                line[strcspn(line, "\n")] = 0;
                
                char cmd[1024];
                snprintf(cmd, sizeof(cmd), "setprop %s >/dev/null 2>&1 &", line);
                system(cmd);
            }
            fclose(prop_file);
        }
    }
    closedir(dir);
}

void isolate_denylist() {
    FILE *deny_file = fopen(DENYLIST_PATH, "r");
    if (!deny_file) return;

    char deny_uids[256][16];
    int deny_count = 0;
    char line[32];

    while (fgets(line, sizeof(line), deny_file) && deny_count < 256) {
        line[strcspn(line, "\n")] = 0;
        if (strlen(line) > 0) {
            strncpy(deny_uids[deny_count], line, 15);
            deny_count++;
        }
    }
    fclose(deny_file);

    if (deny_count == 0) return;

    DIR *proc_dir = opendir("/proc");
    if (!proc_dir) return;

    struct dirent *entry;
    while ((entry = readdir(proc_dir)) != NULL) {
        if (entry->d_name[0] < '0' || entry->d_name[0] > '9') continue;

        int pid = atoi(entry->d_name);
        if (pid <= 1) continue;

        char status_path[64];
        snprintf(status_path, sizeof(status_path), "/proc/%d/status", pid);
        FILE *status_file = fopen(status_path, "r");
        if (!status_file) continue;

        char buf[256];
        int uid = -1;
        while (fgets(buf, sizeof(buf), status_file)) {
            if (strncmp(buf, "Uid:", 4) == 0) {
                sscanf(buf + 4, "%d", &uid);
                break;
            }
        }
        fclose(status_file);

        if (uid != -1) {
            char uid_str[16];
            sprintf(uid_str, "%d", uid);
            
            for (int i = 0; i < deny_count; i++) {
                if (strcmp(uid_str, deny_uids[i]) == 0) {
                    char cmd[512];
                    snprintf(cmd, sizeof(cmd), "nsenter -t %d -m -- sh -c 'cat /proc/%d/mounts | grep nexussu | cut -d \\  -f 2 | xargs -r umount -l 2>/dev/null' >/dev/null 2>&1 &", pid, pid);
                    system(cmd);
                    break;
                }
            }
        }
    }
    closedir(proc_dir);
}

int main() {
    // NEW: Inject NexusSU bin into PATH so module scripts use BusyBox
    const char *old_path = getenv("PATH");
    char new_path[512];
    if (old_path) {
        snprintf(new_path, sizeof(new_path), "/data/adb/nexussu/bin:%s", old_path);
    } else {
        snprintf(new_path, sizeof(new_path), "/data/adb/nexussu/bin:/system/bin:/system/xbin:/vendor/bin");
    }
    setenv("PATH", new_path, 1);

    // Register as manager and escalate to get MAC bypass
    prctl(NEXUSSU_PRCTL_MAGIC, CMD_REGISTER_MANAGER, 0, 0, 0);
    prctl(NEXUSSU_PRCTL_MAGIC, CMD_ESCALATE_SELF, 0, 0, 0);

    // 1. Re-apply saved root grants to the kernel
    apply_saved_root_grants();
    
    // 2. Execute all active module service.sh scripts
    execute_module_scripts();
    
    // 3. Apply all active module system.prop files
    apply_module_props();
    
    // 4. Background loop to isolate denylisted apps (MagiskHide)
    while(1) {
        isolate_denylist();
        sleep(2); // Poll every 2 seconds
    }
    
    return 0;
}
