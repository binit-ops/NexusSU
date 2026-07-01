properties() { 'kernel.string=NexusSU; do.devicecheck=1;'; }
block=/dev/block/bootdevice/by-name/boot;
. tools/ak3-core.sh;
dump_boot;
write_boot;
