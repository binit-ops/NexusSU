CC = $(CROSS_COMPILE)gcc
OBJCOPY = $(CROSS_COMPILE)objcopy
CFLAGS = -Wall -O2 -nostdlib -fforce-mem -fno-stack-protector -fPIC

all: nexus_su.bin

nexus_su.elf: injector.c
	$(CC) $(CFLAGS) -Ttext 0x0 -o nexus_su.elf injector.c

nexus_su.bin: nexus_su.elf
	$(OBJCOPY) -O binary -R .note -R .comment -S nexus_su.elf nexus_su.bin

clean:
	rm -f *.elf *.bin *.o
