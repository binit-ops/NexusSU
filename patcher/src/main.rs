// patcher/src/main.rs (Append this to your existing code)

fn find_code_cave(data: &[u8], size: usize) -> Option<usize> {
    // Scan for a block of null bytes large enough for our payload
    let mut count = 0;
    for (i, &byte) in data.iter().enumerate() {
        if byte == 0 {
            count += 1;
            if count >= size {
                return Some(i - size + 1); // Return the start of the cave
            }
        } else {
            count = 0;
        }
    }
    None
}

fn inject_payload(data: &mut MmapMut, offset: usize, payload: &[u8]) {
    // Copy the payload binary into the identified memory cave
    data[offset..offset + payload.len()].copy_from_slice(payload);
    println!("[+] Nexus SU payload injected at offset 0x{:X}", offset);
}
