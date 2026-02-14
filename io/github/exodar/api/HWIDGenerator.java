/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * HWID Generator that matches the C++ launcher's HWID generation
 * Uses WMIC commands to collect hardware info and generates SHA512 hash
 */
public class HWIDGenerator {

    private static String cachedHWID = null;

    /**
     * Get or generate HWID hash (cached after first generation)
     */
    public static String getHWIDHash() {
        if (cachedHWID != null) {
            return cachedHWID;
        }
        cachedHWID = generateHWIDHash();
        return cachedHWID;
    }

    /**
     * Force regenerate HWID (bypasses cache)
     */
    public static String regenerateHWIDHash() {
        cachedHWID = generateHWIDHash();
        return cachedHWID;
    }

    /**
     * Generate HWID hash matching the launcher's algorithm:
     * SHA512(cpu_id + "|" + motherboard_serial + "|" + gpu_serial + "|" + ram_serial + "|" + disk_serial)
     */
    private static String generateHWIDHash() {
        try {
            // System.out.println("[HWIDGenerator] Generating HWID...");

            // Collect hardware info (same as launcher)
            String cpuId = getCPUID();
            String motherboardSerial = getMotherboardSerial();
            String gpuSerial = getGPUSerial();
            String ramSerial = getRAMSerial();
            String diskSerial = getDiskSerial();

            // Combine exactly like the launcher does
            String combined = cpuId + "|" + motherboardSerial + "|" + gpuSerial + "|" + ramSerial + "|" + diskSerial;

            // System.out.println("[HWIDGenerator] CPU: " + (cpuId.length() > 16 ? cpuId.substring(0, 16) + "..." : cpuId));
            // System.out.println("[HWIDGenerator] MB: " + motherboardSerial);
            // System.out.println("[HWIDGenerator] GPU: " + (gpuSerial.length() > 20 ? gpuSerial.substring(0, 20) + "..." : gpuSerial));
            // System.out.println("[HWIDGenerator] RAM: " + ramSerial);
            // System.out.println("[HWIDGenerator] Disk: " + diskSerial);

            // SHA512 hash (same as launcher)
            String hash = sha512(combined);
            // System.out.println("[HWIDGenerator] HWID Hash: " + hash.substring(0, 32) + "...");

            return hash;

        } catch (Exception e) {
            // System.out.println("[HWIDGenerator] Error generating HWID: " + e.getMessage());
            e.printStackTrace();
            // Fallback: use basic system properties
            return sha512(System.getProperty("os.name") + System.getProperty("user.name") + System.getenv("COMPUTERNAME"));
        }
    }

    /**
     * Get CPU ID using PowerShell WMI
     * Matches launcher's CPUID + WMI ProcessorId combination
     */
    private static String getCPUID() {
        try {
            // Get ProcessorId from WMI (main identifier)
            String wmiCpu = runWMIQuery("Win32_Processor", "ProcessorId");

            StringBuilder cpuId = new StringBuilder();

            // Add processor identifier from environment (like launcher's CPUID intrinsic)
            String procIdentifier = System.getenv("PROCESSOR_IDENTIFIER");
            if (procIdentifier != null && !procIdentifier.isEmpty()) {
                cpuId.append(procIdentifier.hashCode() < 0 ?
                    Integer.toHexString(procIdentifier.hashCode()).substring(0, 8) :
                    String.format("%08x", procIdentifier.hashCode()));
            }

            if (!wmiCpu.isEmpty()) {
                if (cpuId.length() > 0) cpuId.append("-");
                cpuId.append(wmiCpu);
            }

            return cpuId.length() > 0 ? cpuId.toString() : "UNKNOWN_CPU";

        } catch (Exception e) {
            return "UNKNOWN_CPU";
        }
    }

    /**
     * Get motherboard serial using PowerShell WMI
     */
    private static String getMotherboardSerial() {
        try {
            String serial = runWMIQuery("Win32_BaseBoard", "SerialNumber");
            return !serial.isEmpty() ? serial : "UNKNOWN_MB";
        } catch (Exception e) {
            return "UNKNOWN_MB";
        }
    }

    /**
     * Get GPU identifier using PowerShell WMI
     * Launcher uses PNPDeviceID or DeviceID
     */
    private static String getGPUSerial() {
        try {
            // Try PNPDeviceID first (like launcher)
            String pnpId = runWMIQuery("Win32_VideoController", "PNPDeviceID");
            if (!pnpId.isEmpty()) {
                return pnpId;
            }

            // Fallback to DeviceID
            String deviceId = runWMIQuery("Win32_VideoController", "DeviceID");
            return !deviceId.isEmpty() ? deviceId : "UNKNOWN_GPU";

        } catch (Exception e) {
            return "UNKNOWN_GPU";
        }
    }

    /**
     * Get RAM serial numbers using PowerShell WMI
     * Launcher concatenates all RAM stick serials with "|"
     */
    private static String getRAMSerial() {
        try {
            String serials = runWMIQuery("Win32_PhysicalMemory", "SerialNumber");
            return !serials.isEmpty() ? serials : "UNKNOWN_RAM";
        } catch (Exception e) {
            return "UNKNOWN_RAM";
        }
    }

    /**
     * Get disk serial using PowerShell WMI
     */
    private static String getDiskSerial() {
        try {
            String serial = runWMIQuery("Win32_DiskDrive", "SerialNumber");
            return !serial.isEmpty() ? serial : "UNKNOWN_DISK";
        } catch (Exception e) {
            return "UNKNOWN_DISK";
        }
    }

    /**
     * Run WMI query using PowerShell (works on all modern Windows)
     * @param wmiClass The WMI class (e.g., "Win32_Processor", "Win32_BaseBoard")
     * @param property The property to retrieve
     * @return The value, or empty string if failed
     */
    private static String runWMIQuery(String wmiClass, String property) {
        try {
            // Use PowerShell to query WMI - works on Windows 10/11 where WMIC may be removed
            String psCommand = String.format(
                "(Get-WmiObject -Class %s).%s",
                wmiClass, property
            );

            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", psCommand
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    if (output.length() > 0) output.append("|");
                    output.append(line);
                }
            }

            process.waitFor();
            reader.close();

            String result = output.toString().trim();
            // Debug log
            if (!result.isEmpty() && !result.contains("Exception") && !result.contains("error")) {
                return result;
            }
            return "";

        } catch (Exception e) {
            // System.out.println("[HWIDGenerator] PowerShell error for " + wmiClass + ": " + e.getMessage());
            return "";
        }
    }

    /**
     * SHA512 hash (matches launcher's Crypto::SHA512)
     */
    private static String sha512(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            // System.out.println("[HWIDGenerator] SHA512 error: " + e.getMessage());
            return "";
        }
    }

    // ===============================================
    // HWID2 - Simple HWID (SHA256 of C: volume serial)
    // Compatible with launcher's GenerateSimpleHWID()
    // ===============================================

    private static String cachedHWID2 = null;

    /**
     * Get or generate HWID2 (cached after first generation)
     * HWID2 = SHA256(uppercase_hex_volume_serial_of_C_drive)
     */
    public static String getHWID2() {
        if (cachedHWID2 != null) {
            return cachedHWID2;
        }
        cachedHWID2 = generateHWID2();
        return cachedHWID2;
    }

    /**
     * Generate HWID2 - matches the launcher's GenerateSimpleHWID()
     * Formula: SHA256(uppercase_hex_volume_serial_of_C_drive)
     */
    private static String generateHWID2() {
        try {
            String volumeSerial = getCDriveVolumeSerial();
            if (volumeSerial == null || volumeSerial.isEmpty()) {
                // System.out.println("[HWIDGenerator] Failed to get C: drive volume serial for HWID2");
                return null;
            }
            // System.out.println("[HWIDGenerator] Volume Serial for HWID2: " + volumeSerial);
            return sha256(volumeSerial);
        } catch (Exception e) {
            // System.out.println("[HWIDGenerator] Error generating HWID2: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get C: drive volume serial number
     * Returns uppercase hex string (e.g., "ABCD1234")
     */
    private static String getCDriveVolumeSerial() {
        try {
            // Method 1: Using vol command
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "vol", "C:");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                // System.out.println("[HWIDGenerator] vol output: " + line);
                // Look for serial pattern XXXX-XXXX (works with any language)
                // English: "Volume Serial Number is XXXX-XXXX"
                // Spanish: "El número de serie del volumen es: XXXX-XXXX"
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([0-9A-Fa-f]{4}-[0-9A-Fa-f]{4})").matcher(line);
                if (matcher.find()) {
                    String serial = matcher.group(1).replace("-", "").toUpperCase();
                    // System.out.println("[HWIDGenerator] Found serial via vol: " + serial);
                    reader.close();
                    process.waitFor();
                    return serial;
                }
            }

            reader.close();
            process.waitFor();
        } catch (Exception e) {
            // System.out.println("[HWIDGenerator] vol command failed: " + e.getMessage());
        }

        try {
            // Method 2: Using wmic (fallback)
            // System.out.println("[HWIDGenerator] Trying wmic fallback...");
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c",
                "wmic logicaldisk where deviceid='C:' get volumeserialnumber");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // System.out.println("[HWIDGenerator] wmic output: '" + line + "'");
                // Skip header line and empty lines
                if (!line.isEmpty() && !line.equalsIgnoreCase("VolumeSerialNumber")) {
                    // This should be the serial number (8 hex chars)
                    if (line.matches("[0-9A-Fa-f]{8}")) {
                        // System.out.println("[HWIDGenerator] Found serial via wmic: " + line);
                        reader.close();
                        process.waitFor();
                        return line.toUpperCase();
                    }
                }
            }

            reader.close();
            process.waitFor();
        } catch (Exception e) {
            // System.out.println("[HWIDGenerator] wmic command failed: " + e.getMessage());
        }

        // Method 3: Try PowerShell as last resort
        try {
            // System.out.println("[HWIDGenerator] Trying PowerShell fallback...");
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command",
                "(Get-WmiObject -Class Win32_LogicalDisk -Filter \"DeviceID='C:'\").VolumeSerialNumber");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // System.out.println("[HWIDGenerator] PowerShell output: '" + line + "'");
                if (!line.isEmpty() && line.matches("[0-9A-Fa-f]{8}")) {
                    // System.out.println("[HWIDGenerator] Found serial via PowerShell: " + line);
                    reader.close();
                    process.waitFor();
                    return line.toUpperCase();
                }
            }

            reader.close();
            process.waitFor();
        } catch (Exception e) {
            // System.out.println("[HWIDGenerator] PowerShell command failed: " + e.getMessage());
        }

        // System.out.println("[HWIDGenerator] All methods failed to get volume serial");
        return null;
    }

    /**
     * SHA256 hash (for HWID2)
     */
    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            // System.out.println("[HWIDGenerator] SHA256 error: " + e.getMessage());
            return "";
        }
    }

    /**
     * Get individual hardware components (for debugging)
     */
    public static String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== HWID Debug Info ===\n");
        sb.append("CPU ID: ").append(getCPUID()).append("\n");
        sb.append("Motherboard: ").append(getMotherboardSerial()).append("\n");
        sb.append("GPU: ").append(getGPUSerial()).append("\n");
        sb.append("RAM: ").append(getRAMSerial()).append("\n");
        sb.append("Disk: ").append(getDiskSerial()).append("\n");
        sb.append("HWID Hash: ").append(getHWIDHash()).append("\n");
        sb.append("=== HWID2 (Simple) ===\n");
        sb.append("Volume Serial: ").append(getCDriveVolumeSerial()).append("\n");
        sb.append("HWID2: ").append(getHWID2()).append("\n");
        return sb.toString();
    }
}
