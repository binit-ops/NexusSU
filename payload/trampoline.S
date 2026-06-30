.global _nexus_trampoline
.text

// This is the entry point where the hijacked kernel execution lands
_nexus_trampoline:
    // Step 1: Save the CPU state (Registers x0-x3, plus Frame Pointer and Link Register)
    // If we don't save these, our C code will overwrite them and cause a kernel panic.
    STP x0, x1, [sp, #-16]!
    STP x2, x3, [sp, #-16]!
    STP x29, x30, [sp, #-16]!

    // Step 2: Call your C function (grant_nexus_su_root)
    // (In a complete implementation, x0 would hold the address of the credentials structure)
    BL grant_nexus_su_root

    // Step 3: Restore the CPU state back to exactly how it was before we interrupted it
    LDP x29, x30, [sp], #16
    LDP x2, x3, [sp], #16
    LDP x0, x1, [sp], #16

    // Step 4: Return execution back to the normal Linux kernel flow
    RET
