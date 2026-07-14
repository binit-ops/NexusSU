#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <time.h>
#include <sys/types.h>
#include <sys/syscall.h>

#define LOG_PATH "/data/adb/nexussu/logs.txt"
#define RESPONSE_FILE "/data/local/tmp/.nexussu_response"

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
    // 1. Check if we already have root
    if (getuid() != 0) {
        // 2. Check if we've already requested (prevents infinite loop)
        if (getenv("NEXUSSU_REQUESTED") != NULL) {
            fprintf(stderr, "su: root access denied.\n");
            return 1;
        }

        // 3. Generate a secure random PIN (using getrandom syscall)
        unsigned int pin;
        syscall(SYS_getrandom, &pin, sizeof(pin), 0);
        pin = pin % 900000 + 100000; // 6-digit PIN

        // 4. Launch the request dialog in the Manager App with the PIN
        char am_cmd[512];
        sprintf(am_cmd, "am start -n com.nexussu.manager/.SuRequestActivity --es caller_uid %d --es pin %d >/dev/null 2>&1", getuid(), pin);
        system(am_cmd);

        // 5. Poll for response file containing the exact PIN (up to 30 seconds)
        char expected_response[32];
        sprintf(expected_response, "%d", pin);
        int granted = 0;

        for (int i = 0; i < 30; i++) {
            FILE *f = fopen(RESPONSE_FILE, "r");
            if (f) {
                char buf[32];
                if (fgets(buf, sizeof(buf), f) != NULL) {
                    if (strcmp(buf, expected_response) == 0) {
                        granted = 1;
                    }
                }
                fclose(f);
                remove(RESPONSE_FILE);
                break;
            }
            sleep(1);
        }

        if (!granted) {
            fprintf(stderr, "su: request denied or timed out.\n");
            return 1;
        }

        // 6. Re-exec ourselves with the env var to prevent loops
        setenv("NEXUSSU_REQUESTED", "1", 1);
        execv("/system/bin/su", argv);

        fprintf(stderr, "su: failed to re-exec.\n");
        return 1;
    }

    // We have root! Log the access and proceed.
    log_access();

    char *shell = "/system/bin/sh";
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
