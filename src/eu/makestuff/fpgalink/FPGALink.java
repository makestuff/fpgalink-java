package eu.makestuff.fpgalink;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.PointerByReference;

public final class FPGALink {
	private static interface NativeLib extends Library {
		int flInitialise(int debugLevel, PointerByReference pError);
		void flFreeError(Pointer error);

		int flOpen(String vp, PointerByReference pHandle, PointerByReference pError);
		void flClose(Pointer handle);

		int flIsDeviceAvailable(String vp, ByteByReference isAvailable, PointerByReference pError);
		int flProgram(Pointer handle, String portConfig, String progFile, PointerByReference pError);
		int flFifoMode(Pointer handle, byte fifoMode, PointerByReference pError);
		
		int flReadChannel(Pointer handle, int timeout, byte channel, int count, Pointer buffer, PointerByReference pError);
		int flWriteChannel(Pointer handle, int timeout, byte channel, int count, Pointer data, PointerByReference pError);
		int flLoadStandardFirmware(String curVidPid, String newVidPid, PointerByReference pError);

		int flPortConfig(Pointer handle, String portConfig, PointerByReference pError);
		
		void flSleep(int ms);
	}

	private static final NativeLib LIB;
	private static Memory m_buffer = null;

	static {
		LIB = (NativeLib)Native.loadLibrary("fpgalink", NativeLib.class);
		PointerByReference pError = new PointerByReference();
		int retCode = LIB.flInitialise(0, pError);
		checkThrow(retCode, pError);
	}

	private static void checkThrow(int retCode, PointerByReference pError) {
		if ( retCode != 0 ) {
			Pointer errPtr = pError.getValue();
			String errStr = errPtr.getString(0);
			LIB.flFreeError(errPtr);
			throw new FPGALinkException(errStr, retCode);
		}
	}

	private FPGALink() { }
	
	public static final class Connection {

		private final Pointer m_handle;

		private Connection(Pointer handle) {
			m_handle = handle;
		}
		
		public void close() {
			LIB.flClose(m_handle);
		}
		
		@Override
		public void finalize() {
			close();
		}
		
		public void fifoMode(int mode) {
			PointerByReference pError = new PointerByReference();
			int retCode = LIB.flFifoMode(m_handle, (byte)mode, pError);
			checkThrow(retCode, pError);
		}
		public void program(String progConfig, String progFile) {
			PointerByReference pError = new PointerByReference();
			int retCode = LIB.flProgram(m_handle, progConfig, progFile, pError);
			checkThrow(retCode, pError);
		}
		public void rawReadChannel(int channel, Memory buffer, int count, int timeout) {
			PointerByReference pError = new PointerByReference();
			int retCode = LIB.flReadChannel(m_handle, timeout, (byte)channel, count, buffer, pError);
			checkThrow(retCode, pError);
		}
		public void rawWriteChannel(int channel, Memory buffer, int count, int timeout) {
			PointerByReference pError = new PointerByReference();
			int retCode = LIB.flWriteChannel(m_handle, timeout, (byte)channel, count, buffer, pError);
			checkThrow(retCode, pError);
		}
		public void readChannel(int channel, byte[] buffer, int offset, int count, int timeout) {
			if ( m_buffer == null || m_buffer.size() < count ) {
				m_buffer = new Memory(count);
			}
			rawReadChannel(channel, m_buffer, count, timeout);
			m_buffer.read(0, buffer, offset, count);
		}
		public void writeChannel(int channel, byte[] buffer, int offset, int count, int timeout) {
			if ( m_buffer == null || m_buffer.size() < count ) {
				m_buffer = new Memory(count);
			}
			m_buffer.write(0, buffer, offset, count);
			rawWriteChannel(channel, m_buffer, count, timeout);
		}
		public void portConfig(String portConfig) {
			PointerByReference pError = new PointerByReference();
			int retCode = LIB.flPortConfig(m_handle, portConfig, pError);
			checkThrow(retCode, pError);
		}
	}
	
	public static Connection open(String vp) {
		PointerByReference pError = new PointerByReference();
		PointerByReference pHandle = new PointerByReference();
		int retCode = LIB.flOpen(vp, pHandle, pError);
		checkThrow(retCode, pError);
		return new Connection(pHandle.getValue());
	}
	
	public static void loadStandardFirmware(String curVidPid, String newVidPid) {
		PointerByReference pError = new PointerByReference();
		int retCode = LIB.flLoadStandardFirmware(curVidPid, newVidPid, pError);
		checkThrow(retCode, pError);
	}
	
	public static boolean awaitDevice(String vp, int timeout) {
		boolean isAvailable;
		sleep(1000);
		do {
			sleep(100);
			isAvailable = isDeviceAvailable(vp);
			timeout = timeout - 1;
			if ( isAvailable ) {
				return true;
			}
		} while ( timeout > 0 );
		return false;
	}
	
	public static boolean isDeviceAvailable(String vp) {
		ByteByReference pIsAvail = new ByteByReference();
		PointerByReference pError = new PointerByReference();
		int retCode = LIB.flIsDeviceAvailable(vp, pIsAvail, pError);
		checkThrow(retCode, pError);
		return pIsAvail.getValue() != (byte)0x00;
	}
	
	public static void sleep(int ms) {
		LIB.flSleep(ms);
	}
}
