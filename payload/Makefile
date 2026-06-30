CC = $(CROSS_COMPILE)gcc
OBJCOPY = $(CROSS_COMPILE)objcopy

# Flags for headless ARM64 compilation
CFLAGS = -Wall -O2 -nostdlib -fno-stack-protector -fPIC -fomit-frame-pointer

# Linker flags specifically tailored to strip ELF headers and set our Assembly entry point
LDFLAGS = -nostartfiles -nodefaultlibs -Wl,-Ttext=0x0 -Wl,--build-id=none -e _nexus_trampoline

all: nexus_su.bin

# Compile both the Assembly and C files into a single headless ELF
nexus_su.elf: trampoline.S injector.c
	$(CC) $(CFLAGS) $(LDFLAGS) trampoline.S injector.c -o nexus_su.elf

# Extract pure machine code from the combined text segment
nexus_su.bin: nexus_su.elf
	$(OBJCOPY) -O binary -j .text nexus_su.elf nexus_su.bin

clean:
	rm -f *.elf *.bin *.o
