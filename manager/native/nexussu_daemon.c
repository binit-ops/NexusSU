#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/prctl.h>

#define NEXUSSU_PRCTL_MAGIC 0x4E535553
#define CMD_REGISTER_MANAGER 5
#define CMD_GRANT_UID 1

int main() {
    FILE *file = fopen("/data/adb/nexussu/granted_uids.txt", "r");
    if (!file) return 1; // No grants yet, exit quietly

    char line[32];
    
    // Register ourselves as the manager so we can issue prctl commands
    prctl(NEXUSSU_PRCTL_MAGIC, CMD_REGISTER_MANAGER, 0, 0, 0);

    // Read the file line by line and grant each UID
    while (fgets(line, sizeof(line), file)) {
        int uid = atoi(line);
        if (uid > 0) {
            prctl(NEXUSSU_PRCTL_MAGIC, CMD_GRANT_UID, (unsigned long)uid, 0, 0);
        }
    }
    
    fclose(file);
    return 0;
}
