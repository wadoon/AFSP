package edu.kit.tm.afsp.g1;

/**
 * Einfacher ENUM für die Nachrichtentypen (OpCodes)
 * @author weigla
 *
 */
public enum MessageType {

	SIGNIN(0x01),

	SIGNIN_ACK(0x02), SIGNOUT(0x03), EXCHANGE_FILELIST(0x04), EXCHANGE_FILELIST_REQ(
			0x05), DOWNLOAD_REQ(0x06), DOWNLOAD_RPL(0x07), DOWNLOAD_ERR(0x08), DOWNLOAD_ACK(
			0x09), HEARTBEAT(0x0A), UNKNOWN(0);

	int opcode;

	MessageType(int opcode) {
		this.opcode = opcode;
	}
}