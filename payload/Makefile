CC = $(CROSS_COMPILE)gcc
OBJCOPY = $(CROSS_COMPILE)objcopy

# Compiler flags for raw memory injection
CFLAGS = -Wall -O2 -nostdlib -fno-stack-protector -fPIC

# Linker flags: 
# -N: Do not page-align data (fixes PHDR error)
# --build-id=none: Removes GNU build notes
# -e grant_nexus_su_root: Defines our custom entry point instead of _start
LDFLAGS = -Wl,-N -Wl,--build-id=none -Wl,-e,grant_nexus_su_root -Ttext 0x0

all: nexus_su.bin

nexus_su.elf: injector.c
	$(CC) $(CFLAGS) $(LDFLAGS) -o nexus_su.elf injector.c

nexus_su.bin: nexus_su.elf
	$(OBJCOPY) -O binary -R .note -R .comment -S nexus_su.elf nexus_su.bin

clean:
	rm -f *.elf *.bin *.o
