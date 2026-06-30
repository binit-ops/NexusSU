CC = $(CROSS_COMPILE)gcc
OBJCOPY = $(CROSS_COMPILE)objcopy

# -c tells GCC to compile to an object file without invoking the linker
# -fomit-frame-pointer keeps the assembly minimal and clean
CFLAGS = -Wall -O2 -nostdlib -fno-stack-protector -fPIC -fomit-frame-pointer

all: nexus_su.bin

# Step 1: Compile C into an Object File (.o)
injector.o: injector.c
	$(CC) $(CFLAGS) -c injector.c -o injector.o

# Step 2: Extract ONLY the raw executable instructions (.text) into the final .bin
nexus_su.bin: injector.o
	$(OBJCOPY) -O binary -j .text injector.o nexus_su.bin

clean:
	rm -f *.o *.bin
