#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

/*
 * NexusSU Real su Binary
 * When an app executes this, the kernel hook intercepts it and grants UID 0.
 * This binary then launches the standard Android shell as root.
 */

int main(int argc, char *argv[]) {
    char *shell = "/system/bin/sh";
    
    // If arguments are provided (e.g., "su -c 'id'"), execute them
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
    
    // If no arguments, drop into an interactive root shell
    execl(shell, shell, NULL);
    perror("su: exec failed");
    return 1;
}
