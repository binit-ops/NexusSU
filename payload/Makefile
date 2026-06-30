CC = $(CROSS_COMPILE)gcc
OBJCOPY = $(CROSS_COMPILE)objcopy
# Removed the deprecated -fforce-mem flag
CFLAGS = -Wall -O2 -nostdlib -fno-stack-protector -fPIC

all: nexus_su.bin

nexus_su.elf: injector.c
	$(CC) $(CFLAGS) -Ttext 0x0 -o nexus_su.elf injector.c

nexus_su.bin: nexus_su.elf
	$(OBJCOPY) -O binary -R .note -R .comment -S nexus_su.elf nexus_su.bin

clean:
	rm -f *.elf *.bin *.o
