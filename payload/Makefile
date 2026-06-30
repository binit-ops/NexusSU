CC = $(CROSS_COMPILE)gcc
OBJCOPY = $(CROSS_COMPILE)objcopy
CFLAGS = -Wall -O2 -nostdlib -fforce-mem -fno-stack-protector -fPIC

all: nexus.bin

nexus.elf: injector.c
	$(CC) $(CFLAGS) -Ttext 0x0 -o nexus.elf injector.c

	nexus.bin: nexus.elf
		$(OBJCOPY) -O binary -R .note -R .comment -S nexus.elf nexus.bin

		clean:
			rm -f *.elf *.bin *.o
			