#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <time.h>
#include <sys/types.h>
#include <sys/syscall.h>
#include <sys/wait.h>
#include <sys/prctl.h>

#define LOG_PATH "/data/adb/nexussu/logs.txt"
#define RESPONSE_FILE_TEMPLATE "/data/local/tmp/.nexussu_response_%d"

void log_access() {
    FILE *file = fopen(LOG_PATH, "a");
    if (file) {
        time_t now = time(NULL);
        struct tm *t = localtime(&now);
        char time_str[64];
        strftime(time_str, sizeof(time_str), "%Y-%m-%d %H:%M:%S", t);
        fprintf(file, "[%s] UID=%d PID=%d\n", time_str, getuid(), getpid());
        fclose(file);
    }
}

// Environment Sanitization to prevent root detection and library conflicts
void sanitize_environment() {
    // Wipe app-specific environment variables
    unsetenv("LD_LIBRARY_PATH");
    unsetenv("CLASSPATH");
    unsetenv("ANDROID_DATA");
    unsetenv("ANDROID_ROOT");
    unsetenv("BOOTCLASSPATH");
    unsetenv("ANDROID_ASSETS");
    unsetenv("ANDROID_BOOTLOGO");
    unsetenv("EXTERNAL_STORAGE");
    unsetenv("ANDROID_STORAGE");
    unsetenv("ASEC_MOUNTPOINT");
    unsetenv("LOOP_MOUNTPOINT");
    unsetenv("NEXUSSU_REQUESTED"); // Remove our internal tracking variable

    // Restore standard root environment
    setenv("PATH", "/system/bin:/system/xbin:/sbin:/vendor/bin", 1);
    setenv("HOME", "/data", 1);
    setenv("USER", "root", 1);
    setenv("LOGNAME", "root", 1);
    setenv("SHELL", "/system/bin/sh", 1);
}

int main(int argc, char *argv[]) {
    // 1. Check if we already have root
    if (getuid() != 0) {
        // Check if we've already requested (prevents infinite loop)
        if (getenv("NEXUSSU_REQUESTED") != NULL) {
            fprintf(stderr, "su: root access denied.\n");
            return 1;
        }

        // Generate a secure random PIN
        unsigned int pin;
        syscall(SYS_getrandom, &pin, sizeof(pin), 0);
        pin = pin % 900000 + 100000;

        // Unique response file based on PID to prevent concurrent collisions
        pid_t pid = getpid();
        char response_file[64];
        sprintf(response_file, RESPONSE_FILE_TEMPLATE, pid);

        // Hardened: Use absolute path and fork/exec to prevent PATH hijacking
        char am_cmd[512];
        sprintf(am_cmd, "/system/bin/am start -n com.nexussu.manager/.SuRequestActivity --es caller_uid %d --es pin %d --es pid %d >/dev/null 2>&1", getuid(), pin, pid);
        
        pid_t am_pid = fork();
        if (am_pid == 0) {
            setenv("PATH", "/system/bin:/system/xbin:/vendor/bin", 1);
            execl("/system/bin/sh", "sh", "-c", am_cmd, NULL);
            _exit(1);
        }

        // Poll for unique response file containing the exact PIN (up to 30 seconds)
        char expected_response[32];
        sprintf(expected_response, "%d", pin);
        int granted = 0;

        for (int i = 0; i < 30; i++) {
            FILE *f = fopen(response_file, "r");
            if (f) {
                char buf[32];
                if (fgets(buf, sizeof(buf), f) != NULL) {
                    if (strcmp(buf, expected_response) == 0) {
                        granted = 1;
                    }
                }
                fclose(f);
                remove(response_file);
                break;
            }
            sleep(1);
        }

        if (!granted) {
            fprintf(stderr, "su: request denied or timed out.\n");
            return 1;
        }

        // Re-exec ourselves with the env var to prevent loops
        setenv("NEXUSSU_REQUESTED", "1", 1);
        execv("/system/bin/su", argv);

        fprintf(stderr, "su: failed to re-exec.\n");
        return 1;
    }

    // --- WE HAVE ROOT ---

    // Log the access
    log_access();

    // Sanitize the environment before dropping to the shell
    sanitize_environment();

    // Spoof the process name to hide from `ps` and `top` (looks like a kernel thread)
    prctl(PR_SET_NAME, "kthreadd", 0, 0, 0);

    char *shell = "/system/bin/sh";
    
    // Execute command if provided (e.g., su -c "id")
    if (argc >= 3 && strcmp(argv[1], "-c") == 0) {
        char cmd[4096] = {0};
        for (int i = 2; i < argc; i++) {
            strcat(cmd, argv[i]);
            strcat(cmd, " ");
        }
        // Pass "kthreadd" as argv[0] so the shell process also inherits the fake name
        execl(shell, "kthreadd", "-c", cmd, NULL);
        perror("su: exec failed");
        return 1;
    }
    
    // Drop into interactive shell
    execl(shell, "kthreadd", NULL);
    perror("su: exec failed");
    return 1;
}
