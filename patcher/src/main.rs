use std::fs::OpenOptions;
use std::io;
use memmap2::MmapMut;

fn locate_pattern(data: &[u8], pattern: &[Option<u8>]) -> Option<usize> {
    if pattern.is_empty() || data.len() < pattern.len() { return None; }
    data.windows(pattern.len()).position(|window| {
        window.iter().zip(pattern.iter()).all(|(b, p)| p.map_or(true, |expected| *b == expected))
    })
}

fn main() -> io::Result<()> {
    println!("=== Nexus SU Boot Initialization ===");
    
    let file = match OpenOptions::new().read(true).write(true).open("Image") {
        Ok(f) => f,
        Err(_) => {
            println!("[-] Waiting for 'Image' binary to begin testing.");
            return Ok(());
        }
    };

    let mmap = unsafe { MmapMut::map_mut(&file)? };
    println!("[+] Kernel mapped to memory: {} bytes.", mmap.len());

    let dispatch_sig: Vec<Option<u8>> = vec![
        Some(0x08), None, None, Some(0x90), // adrp x8, table
        Some(0x08), None, None, Some(0x91), // add x8, x8, table
    ];

    match locate_pattern(&mmap, &dispatch_sig) {
        Some(offset) => println!("[+] Syscall routing vector matched at 0x{:X}", offset),
        None => println!("[-] Signature not found."),
    }
    
    Ok(())
}
