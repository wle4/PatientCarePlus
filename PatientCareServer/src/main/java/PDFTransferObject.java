import java.io.File;
import java.io.Serializable;

public class PDFTransferObject implements Serializable {
    private int senderId;
    private int receiverId;
    private byte[] pdfData;
    private String fileName;

    public PDFTransferObject(int senderId, int receiverId, byte[] pdfData, String fileName) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.pdfData = pdfData;
        this.fileName = fileName;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public byte[] getPdfData() {
        return pdfData;
    }

    public String getFileName() {
        return fileName;
    }
}