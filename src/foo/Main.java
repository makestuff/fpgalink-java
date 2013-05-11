package foo;

import com.sun.jna.Memory;

import eu.makestuff.fpgalink.FPGALink;
import eu.makestuff.fpgalink.FPGALinkException;

public class Main {

	// The VID:PID:DID that the device will renumerate as (the device-ID "DID" is optional).
	private static final String ACTUAL_VIDPID = "1d50:602b:0009";

	// The initial VID:PID of the device; this depends on the vendor
	private static final String INIT_VIDPID = "1443:0005";  // e.g Nexys2
	//private static final String INIT_VIDPID = "1443:0007";  // e.g Atlys
	//private static final String INIT_VIDPID = "04b4:8613";  // e.g default FX2LP

	// The programming configuration
	//private static final String PROG_CONFIG = "J:A7A0A3A1:../hdl/apps/makestuff/swled/cksum/vhdl/top_level.xsvf";  // e.g MakeStuff LX9
	private static final String PROG_CONFIG = "J:D0D2D3D4:../hdl/apps/makestuff/swled/cksum/vhdl/top_level.xsvf";  // e.g Digilent boards

	// Where to load the native FPGALink libraries from
	private static final String JNA_PATH = "../lin.x64/rel/";
	
	public static void main(String[] args) {
		FPGALink.Connection conn = null;
		try {
			System.setProperty("jna.library.path", JNA_PATH);
			
			// Open an FPGALink connection
			try {
				System.out.println("Attempting to open connection to FPGALink device " + ACTUAL_VIDPID + "...");
				conn = FPGALink.open(ACTUAL_VIDPID);
			}
			catch ( FPGALinkException ex ) {
				System.out.println("Loading firmware into " + INIT_VIDPID + "...");
				FPGALink.loadStandardFirmware(INIT_VIDPID, ACTUAL_VIDPID);
				System.out.println("Awaiting renumeration as " + ACTUAL_VIDPID + "...");
				if ( !FPGALink.awaitDevice(ACTUAL_VIDPID, 600) ) {
					throw new FPGALinkException("FPGALink device did not renumerate properly as " + ACTUAL_VIDPID, -1);
				}
				System.out.println("Attempting to open connection to FPGALink device " + ACTUAL_VIDPID + " again...");
				conn = FPGALink.open(ACTUAL_VIDPID);
			}

			// This enables the Nexys2 power FET. It is only needed on Nexys2 boards
			System.out.println("Configuring ports on FPGALink device " + ACTUAL_VIDPID + "...");
			conn.portConfig("D7+");
			FPGALink.sleep(100);

			// Program the FPGA
			System.out.println("Programming FPGA connected to FPGALink device " + ACTUAL_VIDPID + "...");
			conn.program(PROG_CONFIG, null);
			conn.fifoMode(1);

			// Try some raw read/write operations...
			System.out.println("Trying raw reads/writes...");
			Memory rawBuf = new Memory(4);
			rawBuf.setByte(0, (byte)0x00);
			conn.rawWriteChannel(1, rawBuf, 1, 1000);
			conn.rawWriteChannel(2, rawBuf, 1, 1000);
			rawBuf.setByte(0, (byte)0x23);
			rawBuf.setByte(1, (byte)0x42);
			rawBuf.setByte(2, (byte)0xCA);
			rawBuf.setByte(3, (byte)0xFE);
			conn.rawWriteChannel(0, rawBuf, 4, 1000);
			conn.rawReadChannel(1, rawBuf, 1, 1000);
			byte msb = rawBuf.getByte(0);
			conn.rawReadChannel(2, rawBuf, 1, 1000);
			byte lsb = rawBuf.getByte(0);
			System.out.println(
				"Read checksum value: " +
				String.format("%02X", msb) +
				String.format("%02X", lsb));
			
			// Try some managed read/write operations...
			System.out.println("Trying managed reads/writes...");
			byte[] byteBuf = new byte[4];
			byteBuf[0] = (byte)0x00;
			conn.writeChannel(1, byteBuf, 0, 1, 1000);
			conn.writeChannel(2, byteBuf, 0, 1, 1000);
			byteBuf[0] = (byte)0x23;
			byteBuf[1] = (byte)0x42;
			byteBuf[2] = (byte)0xCA;
			byteBuf[3] = (byte)0xFE;
			conn.writeChannel(0, byteBuf, 0, 4, 1000);
			conn.readChannel(1, byteBuf, 0, 1, 1000);
			conn.readChannel(2, byteBuf, 1, 1, 1000);
			System.out.println(
				"Read checksum value: " +
				String.format("%02X", byteBuf[0]) +
				String.format("%02X", byteBuf[1]));
		}
		catch ( FPGALinkException ex ) {
			ex.printStackTrace();
		}
		finally {
			// Close FPGALink connection
			if ( conn != null ) {
				System.out.println("Closing the connection...");
				conn.close();
			}
		}
	}
}
