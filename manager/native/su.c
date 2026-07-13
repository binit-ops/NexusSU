#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <time.h>
#include <sys/types.h>

#define LOG_PATH "/data/adb/nexussu/logs.txt"

void log_access() {
    FILE *file = fopen(LOG_PATH, "a");
    if (file) {
        time_t now = time(NULL);
        struct tm *t = localtime(&now);
        char time_str[64];
        strftime(time_str, sizeof(time_str), "%Y-%m-%d %H:%M:%S", t);
        
        // Log format: [Timestamp] UID=XXXX PID=XXXX
        fprintf(file, "[%s] UID=%d PID=%d\n", time_str, getuid(), getpid());
        fclose(file);
    }
}

int main(int argc, char *argv[]) {
    char *shell = "/system/bin/sh";
    
    // Log the access attempt before executing the shell
    log_access();
    
    if (argc >= 3 && strcmp(argv[1], "-c") == 0) {
        char cmd[4096] = {0};
        for (int i = 2; i < argc; i++) {
            strcat(cmd, argv[i]);
            strcat(cmd, " ");
        }
        execl(shell, shell, "-c", cmd, NULL);
        perror("su: exec failed");
        return 1;
    }
    
    execl(shell, shell, NULL);
    perror("su: exec failed");
    return 1;
}
