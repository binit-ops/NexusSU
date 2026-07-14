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

#define MODULES_DIR "/data/adb/nexussu/modules"

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
        if (entry->d_name[0] == '.') continue; // Skip hidden files (. and ..)

        char module_path[512];
        snprintf(module_path, sizeof(module_path), "%s/%s", MODULES_DIR, entry->d_name);

        // Check if module is disabled
        char disable_path[600];
        snprintf(disable_path, sizeof(disable_path), "%s/disable", module_path);
        if (access(disable_path, F_OK) == 0) continue; // Skip disabled modules

        // Check if service.sh exists
        char script_path[600];
        snprintf(script_path, sizeof(script_path), "%s/service.sh", module_path);
        if (access(script_path, F_OK) == 0) {
            // Make sure it's executable
            chmod(script_path, 0755);
            
            // Execute it in the background
            char cmd[700];
            snprintf(cmd, sizeof(cmd), "sh %s >/dev/null 2>&1 &", script_path);
            system(cmd);
        }
    }
    closedir(dir);
}

int main() {
    // 1. Re-apply saved root grants to the kernel
    apply_saved_root_grants();
    
    // 2. Execute all active module service.sh scripts
    execute_module_scripts();
    
    return 0;
}
