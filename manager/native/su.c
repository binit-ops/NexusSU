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

    // We have root! Log the access.
    log_access();

    // Spoof the process name to hide from `ps` and `top`
    prctl(PR_SET_NAME, "kthreadd", 0, 0, 0);

    // NEW: Prepend NexusSU bin to PATH for BusyBox applet priority
    const char *old_path = getenv("PATH");
    char new_path[512];
    if (old_path) {
        snprintf(new_path, sizeof(new_path), "/data/adb/nexussu/bin:%s", old_path);
    } else {
        snprintf(new_path, sizeof(new_path), "/data/adb/nexussu/bin:/system/bin:/system/xbin:/vendor/bin");
    }
    setenv("PATH", new_path, 1);

    char *shell = "/system/bin/sh";
    
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
    
    // Interactive shell
    execl(shell, "kthreadd", NULL);
    perror("su: exec failed");
    return 1;
}
