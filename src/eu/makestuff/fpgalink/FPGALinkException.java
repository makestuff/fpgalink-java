package eu.makestuff.fpgalink;

public class FPGALinkException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public final int RetCode;
	public FPGALinkException(String msg, int retCode) {
		super(msg);
		RetCode = retCode;
	}
}
