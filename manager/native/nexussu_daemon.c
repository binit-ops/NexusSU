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
#define CMD_WAIT_FOR_DENY_PID 8

#define MODULES_DIR "/data/adb/nexussu/modules"

void apply_saved_root_grants() {
    FILE *file = fopen("/data/adb/nexussu/granted_uids.txt", "r");
    if (!file) return;
    char line[32];
    prctl(NEXUSSU_PRCTL_MAGIC, CMD_REGISTER_MANAGER, 0, 0, 0);
    while (fgets(line, sizeof(line), file)) {
        int uid = atoi(line);
        if (uid > 0) prctl(NEXUSSU_PRCTL_MAGIC, CMD_GRANT_UID, (unsigned long)uid, 0, 0);
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

// NEW: Clean up modules pending removal (Deferred Uninstall)
void cleanup_removed_modules() {
    DIR *dir = opendir(MODULES_DIR);
    if (!dir) return;

    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_name[0] == '.') continue;

        char module_path[512];
        snprintf(module_path, sizeof(module_path), "%s/%s", MODULES_DIR, entry->d_name);

        char remove_path[600];
        snprintf(remove_path, sizeof(remove_path), "%s/remove", module_path);
        
        if (access(remove_path, F_OK) == 0) {
            // Run uninstall.sh if it exists
            char script_path[600];
            snprintf(script_path, sizeof(script_path), "%s/uninstall.sh", module_path);
            if (access(script_path, F_OK) == 0) {
                chmod(script_path, 0755);
                char cmd[700];
                snprintf(cmd, sizeof(cmd), "sh %s >/dev/null 2>&1", script_path);
                system(cmd);
            }
            
            // Unmount system files
            char system_path[600];
            snprintf(system_path, sizeof(system_path), "%s/system", module_path);
            if (access(system_path, F_OK) == 0) {
                char cmd[700];
                snprintf(cmd, sizeof(cmd), "find %s -type f | while read file; do target_path=$(echo $file | sed 's|%s|/system|'); umount $target_path 2>/dev/null; done", system_path, system_path);
                system(cmd);
            }
            
            // Delete the folder
            char rm_cmd[700];
            snprintf(rm_cmd, sizeof(rm_cmd), "rm -rf %s", module_path);
            system(rm_cmd);
        }
    }
    closedir(dir);
}

void isolate_pid(int pid) {
    char cmd[512];
    snprintf(cmd, sizeof(cmd), "nsenter -t %d -m -- sh -c 'cat /proc/%d/mounts | grep nexussu | cut -d \\  -f 2 | xargs -r umount -l 2>/dev/null' >/dev/null 2>&1 &", pid, pid);
    system(cmd);
}

int main(int argc, char *argv[]) {
    // Allow the daemon to be run as a one-off command to reset the manager UID
    if (argc > 1 && strcmp(argv[1], "--reset-manager") == 0) {
        prctl(NEXUSSU_PRCTL_MAGIC, CMD_REGISTER_MANAGER, 0, 0, 0); // Temp register as root
        prctl(NEXUSSU_PRCTL_MAGIC, CMD_RESET_MANAGER, 0, 0, 0);
        return 0;
    }

    const char *old_path = getenv("PATH");
    char new_path[512];
    if (old_path) snprintf(new_path, sizeof(new_path), "/data/adb/nexussu/bin:%s", old_path);
    else snprintf(new_path, sizeof(new_path), "/data/adb/nexussu/bin:/system/bin:/system/xbin:/vendor/bin");
    setenv("PATH", new_path, 1);

    // Daemon does NOT register as manager. It runs as UID 0, so kernel allows it.
    prctl(NEXUSSU_PRCTL_MAGIC, CMD_ESCALATE_SELF, 0, 0, 0);

    cleanup_removed_modules(); // NEW: Delete modules pending removal
    apply_saved_root_grants();
    execute_module_scripts();
    apply_module_props();
    
    // Zero Battery Drain MagiskHide Loop
    while(1) {
        // This call blocks (sleeps) until the kernel wakes it up!
        int pid = prctl(NEXUSSU_PRCTL_MAGIC, CMD_WAIT_FOR_DENY_PID, 0, 0, 0);
        if (pid > 0) {
            isolate_pid(pid);
        }
    }
    
    return 0;
}
