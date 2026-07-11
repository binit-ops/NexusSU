<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NexusSU | ROM Integration</title>
    <link rel="stylesheet" href="style.css">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css" rel="stylesheet" />
    <style>
        .code-window { background: #1e1e2e; border-radius: 8px; margin-bottom: 20px; border: 1px solid var(--border); }
        .code-toolbar { background: #181825; padding: 10px 15px; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }
        .code-info { display: flex; gap: 10px; align-items: center; }
        .file-badge { background: var(--accent); color: #fff; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-family: monospace; }
        .search-badge { color: #a6accd; font-size: 12px; font-family: monospace; }
        pre[class*="language-"] { margin: 0; padding: 15px; background: transparent !important; font-size: 14px; }
        .script-block { background: #1e1e2e; color: #a6accd; padding: 15px; border-radius: 8px; font-family: monospace; margin-bottom: 20px; border-left: 4px solid var(--accent); }
    </style>
</head>
<body>
    <nav>
        <a href="index.html">Home</a>
        <a href="hooks.html">Kernel Integration</a>
        <a href="compile.html">Compilation</a>
        <a href="rom_integration.html" class="active">ROM Integration</a>
    </nav>

    <div class="content hero">
        <h1>NexusSU: Native ROM Integration</h1>
        <p>This guide is for OS maintainers and developers building custom ROMs from source (AOSP/LineageOS). By integrating NexusSU natively, the kernel will boot with the root engine and SELinux bypass fully active, requiring no Magisk or AnyKernel3 flashing.</p>

        <!-- STEP 1 -->
        <h3>Step 1: Patch the Kernel Source</h3>
        <p>NexusSU supports pre-GKI kernels (4.9, 4.14, 4.19, 5.4).</p>
        <ol>
            <li>Clone the NexusSU repository.</li>
            <li>Navigate to the <code>kernel/</code> directory.</li>
            <li>Run the automated patcher against your kernel source tree:</li>
        </ol>
        
        <div class="script-block">
            $ chmod +x apply.sh<br>
            $ ./apply.sh /path/to/your/kernel/source
        </div>
        <p><em>Note: If the automated script fails due to heavy ROM source modifications, refer to the <a href="hooks.html">Kernel Integration</a> page for manual patching instructions.</em></p>
        <p>Build your kernel as usual.</p>

        <!-- STEP 2 -->
        <h3>Step 2: Create the Init Script</h3>
        <p>In your device tree (e.g., <code>device/oem/codename/</code>), create a new directory for NexusSU and create the initialization script. This ensures the IOCTL gateway has the correct permissions for the Manager App.</p>
        
        <div class="code-window">
            <div class="code-toolbar">
                <div class="code-info">
                    <span class="file-badge">device/oem/codename/init/init.nexussu.rc</span>
                    <span class="search-badge">📝 Action: Create New File</span>
                </div>
            </div>
            <pre><code class="language-rc">
# NexusSU Native Init Script
# Defines permissions for the IOCTL gateway at boot

on early-init
    chmod 0666 /dev/nexussu
    chown root root /dev/nexussu
            </code></pre>
        </div>

        <!-- STEP 3 -->
        <h3>Step 3: Include the Init Script in your Build</h3>
        <p>In your device's <code>BoardConfig.mk</code> (or equivalent), ensure the init script is copied to the ramdisk:</p>
        
        <div class="code-window">
            <div class="code-toolbar">
                <div class="code-info">
                    <span class="file-badge">device/oem/codename/BoardConfig.mk</span>
                    <span class="search-badge">📝 Action: Append to File</span>
                </div>
            </div>
            <pre><code class="language-makefile">
# Add NexusSU init script
PRODUCT_COPY_FILES += \
    device/oem/codename/init/init.nexussu.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.nexussu.rc
            </code></pre>
        </div>

        <!-- STEP 4 -->
        <h3>Step 4: Provide the `su` Binary (Crucial)</h3>
        <p>Because NexusSU is a source-injection root, apps will attempt to execute <code>su</code> to gain root. Your ROM must provide a dummy <code>su</code> binary in the system path. The kernel hook in <code>fs/exec.c</code> will intercept the execution of this binary and instantly swap the credentials to UID 0 (root) + bypass SELinux.</p>
        
        <ol>
            <li>Download a pre-compiled dummy <code>su</code> binary (or compile a simple C program that does nothing/returns 0).</li>
            <li>Add it to your device tree:</li>
        </ol>

        <div class="code-window">
            <div class="code-toolbar">
                <div class="code-info">
                    <span class="file-badge">device/oem/codename/device.mk</span>
                    <span class="search-badge">📝 Action: Append to File</span>
                </div>
            </div>
            <pre><code class="language-makefile">
# In device/oem/codename/device.mk
PRODUCT_COPY_FILES += \
    device/oem/codename/prebuilt/su:$(TARGET_COPY_OUT_SYSTEM)/bin/su
            </code></pre>
        </div>

        <p>3. Set the correct permissions in an init script or via <code>PRODUCT_COPY_FILES</code>:</p>

        <div class="code-window">
            <div class="code-toolbar">
                <div class="code-info">
                    <span class="file-badge">init.nexussu.rc</span>
                    <span class="search-badge">📝 Action: Append to File</span>
                </div>
            </div>
            <pre><code class="language-rc">
# In init.nexussu.rc
on boot
    chmod 0755 /system/bin/su
    chown root root /system/bin/su
            </code></pre>
        </div>

        <!-- STEP 5 -->
        <h3>Step 5: SELinux Policy (Automated)</h3>
        <p><strong>You do not need to add any custom SELinux policies to your ROM's sepolicy folders.</strong></p>
        <p>NexusSU uses a KernelSU-Next style dynamic MAC bypass. The <code>apply.sh</code> script patches <code>security/selinux/avc.c</code> and <code>security/selinux/hooks.c</code> directly in the kernel.</p>
        <ul>
            <li>Global SELinux remains in <code>Enforcing</code> mode (Passing Play Integrity).</li>
            <li>Only processes executing the <code>su</code> binary (or granted by the Manager) are granted full <code>u:r:su:s0</code> MAC permissions dynamically.</li>
        </ul>

        <hr style="margin: 40px 0; border-color: var(--border);">

        <h3>Summary</h3>
        <ul>
            <li>Run <code>apply.sh</code> on your kernel source.</li>
            <li>Add <code>init.nexussu.rc</code> to your device tree.</li>
            <li>Ensure a dummy <code>su</code> binary is present in <code>/system/bin/</code>.</li>
            <li>Build the ROM!</li>
        </ul>

    </div>
</body>
</html>
