import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Message implements Serializable {
    private static final long serialVersionUID = 42L;

    private String receiver; // who the message is being sent to
    private final String sender; // who the message came from
    private String msg; // the content in the message

    private Integer receiverid;
    private Integer senderid;

    private ArrayList<String> users;

    public Message(String receiver, String sender, String msg, Integer receiverid, Integer senderid) {
        this.receiver = receiver;
        this.sender = sender;
        this.msg = msg;
        this.receiverid = receiverid;
        this.senderid = senderid;
    }

    public Message(String code, ArrayList<String> users) {
        this.sender = code;
        this.users = users;
    }

    public String getReceiver() {
        // returns the username of the person that the message is being sent to
        return receiver;
    }

    public String getSender() {
        // returns the username of the person who sent the message
        return sender;
    }

    public Integer getReceiverId() {
        return receiverid;
    }

    public Integer getSenderId() {
        return senderid;
    }

    public String getMsg() {
        // returns the content in the message
        return msg;
    }

    public ArrayList<String> updateClients() {
        return users;
    }
}
