package foo;

import com.sun.jna.Memory;

import eu.makestuff.fpgalink.FPGALink;
import eu.makestuff.fpgalink.FPGALinkException;

public class Main {

	private static final String INIT_VIDPID = "04b4:8613";
	private static final String ACTUAL_VIDPID = "1d50:602b:0009";
	private static final String JTAG_CONFIG = "A7031";
	private static final String XSVF_FILE = "/home/chris/libfpgalink-20121216/hdl/apps/makestuff/swled/cksum/vhdl/csvf/fx2min-lx9.csvf";
	private static final String JNA_PATH = "lin.x64/rel";
	
	public static void main(String[] args) {
		try {
			System.setProperty("jna.library.path", JNA_PATH);
			
			FPGALink.Connection conn;
			try {
				System.out.println("Attempting to open connection to FPGALink device " + ACTUAL_VIDPID + "...");
				conn = FPGALink.open(ACTUAL_VIDPID);
			}
			catch ( FPGALinkException ex ) {
				System.out.println("Loading firmware into " + INIT_VIDPID + "...");
				FPGALink.loadStandardFirmware(INIT_VIDPID, ACTUAL_VIDPID, JTAG_CONFIG);
				System.out.println("Awaiting renumeration as " + ACTUAL_VIDPID + "...");
				if ( !FPGALink.awaitDevice(ACTUAL_VIDPID, 600) ) {
					throw new FPGALinkException("FPGALink device did not renumerate properly as " + ACTUAL_VIDPID, -1);
				}
				System.out.println("Attempting to open connection to FPGALink device " + ACTUAL_VIDPID + " again...");
				conn = FPGALink.open(ACTUAL_VIDPID);
			}
			System.out.println("Playing " + XSVF_FILE + " into the JTAG chain on FPGALink device " + ACTUAL_VIDPID + "...");
			conn.playXSVF(XSVF_FILE);

			// Try raw read/write operations...
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
			
			// Try managed read/write operations...
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

			// Close connection
			System.out.println("Closing the connection...");
			conn.close();
		}
		catch ( FPGALinkException ex ) {
			ex.printStackTrace();
		}
	}
}
