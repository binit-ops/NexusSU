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

int get_caller_uid() {
    if (getuid() != 0) return getuid();
    pid_t ppid = getppid();
    char path[64];
    sprintf(path, "/proc/%d/status", ppid);
    FILE *f = fopen(path, "r");
    if (!f) return -1;
    char line[256];
    int uid = -1;
    while (fgets(line, sizeof(line), f)) {
        if (strncmp(line, "Uid:", 4) == 0) {
            sscanf(line + 4, "%d", &uid);
            break;
        }
    }
    fclose(f);
    return uid;
}

void log_access(int caller_uid) {
    FILE *file = fopen(LOG_PATH, "a");
    if (file) {
        time_t now = time(NULL);
        struct tm *t = localtime(&now);
        char time_str[64];
        strftime(time_str, sizeof(time_str), "%Y-%m-%d %H:%M:%S", t);
        fprintf(file, "[%s] UID=%d PID=%d\n", time_str, caller_uid, getpid());
        fclose(file);
    }
}

void notify_manager(int caller_uid) {
    if (caller_uid <= 0) return;
    char cmd[256];
    sprintf(cmd, "/system/bin/am broadcast -a com.nexussu.manager.ROOT_GRANTED --es caller_uid %d >/dev/null 2>&1 &", caller_uid);
    system(cmd);
}

int main(int argc, char *argv[]) {
    if (getuid() != 0) {
        if (getenv("NEXUSSU_REQUESTED") != NULL) {
            fprintf(stderr, "su: root access denied.\n");
            return 1;
        }

        unsigned int pin;
        syscall(SYS_getrandom, &pin, sizeof(pin), 0);
        pin = pin % 900000 + 100000;

        pid_t pid = getpid();
        char response_file[64];
        sprintf(response_file, RESPONSE_FILE_TEMPLATE, pid);

        char am_cmd[512];
        sprintf(am_cmd, "/system/bin/am start -n com.nexussu.manager/.SuRequestActivity --es caller_uid %d --es pin %d --es pid %d >/dev/null 2>&1", getuid(), pin, pid);
        
        pid_t am_pid = fork();
        if (am_pid == 0) {
            setenv("PATH", "/system/bin:/system/xbin:/vendor/bin", 1);
            execl("/system/bin/sh", "sh", "-c", am_cmd, NULL);
            _exit(1);
        }

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

        setenv("NEXUSSU_REQUESTED", "1", 1);
        execv("/system/bin/su", argv);

        fprintf(stderr, "su: failed to re-exec.\n");
        return 1;
    }

    // We have root!
    int caller_uid = get_caller_uid();
    log_access(caller_uid);
    notify_manager(caller_uid);

    prctl(PR_SET_NAME, "kthreadd", 0, 0, 0);

    // Prepend NexusSU bin to PATH for BusyBox applet priority
    const char *old_path = getenv("PATH");
    char new_path[512];
    if (old_path) {
        snprintf(new_path, sizeof(new_path), "/data/adb/nexussu/bin:%s", old_path);
    } else {
        snprintf(new_path, sizeof(new_path), "/data/adb/nexussu/bin:/system/bin:/system/xbin:/vendor/bin");
    }
    setenv("PATH", new_path, 1);

    // NEW: Security - Sanitize Environment to prevent Library Hijacking (LD_PRELOAD)
    unsetenv("LD_PRELOAD");
    unsetenv("LD_LIBRARY_PATH");
    unsetenv("LD_DEBUG");

    // Professional Argument Parsing
    char *shell = "/system/bin/sh";
    char *command = NULL;
    int target_uid = 0; // Default to root (0)

    for (int i = 1; i < argc; i++) {
        if ((strcmp(argv[i], "-c") == 0 || strcmp(argv[i], "--command") == 0) && i + 1 < argc) {
            command = argv[i+1];
            i++; // Skip the command string
        } else if ((strcmp(argv[i], "-s") == 0 || strcmp(argv[i], "--shell") == 0) && i + 1 < argc) {
            shell = argv[i+1];
            i++; // Skip the shell path
        } else {
            // Try to parse as target UID (e.g., "su 1000")
            char *endptr;
            long uid_val = strtol(argv[i], &endptr, 10);
            if (*endptr == '\0' && uid_val >= 0) {
                target_uid = (int)uid_val;
            }
        }
    }

    // If a target UID is specified and is not root, drop privileges
    if (target_uid != 0) {
        if (setgid(target_uid) != 0 || setuid(target_uid) != 0) {
            perror("su: failed to drop privileges");
            return 1;
        }
    }

    // Execute the shell
    if (command != NULL) {
        execl(shell, "kthreadd", "-c", command, NULL);
    } else {
        execl(shell, "kthreadd", NULL);
    }

    perror("su: exec failed");
    return 1;
}
