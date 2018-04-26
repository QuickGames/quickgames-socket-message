package quickgames.socket.message;

import com.sun.istack.internal.NotNull;

import java.math.BigInteger;

public class Message implements OnMessage {

    //region STATIC

    private static int m_messageGlobalId = 0;
    private static final byte M_GROUP_SEPARATOR = 0;

    private static byte[] int2bytes(int value) {
        byte[] result;

        byte[] bytes = BigInteger.valueOf(value).toByteArray();
        if (bytes.length > 1)
            result = new byte[]{bytes[1], bytes[0]};
        else
            result = new byte[]{0, bytes[0]};

        return result;
    }

    //endregion

    //region PRIVATE_PROPERTIES

    private SocketMessage m_socketMessage;
    private OnMessage m_onMessageListener;
    private boolean m_closeAfterWork;

    private int m_messageId;

    //endregion

    //region CONSTRUCTOR

    public Message(@NotNull SocketMessage socketMessage, @NotNull OnMessage onMessageListener) {
        m_constructor(socketMessage, onMessageListener, false);
    }

    public Message(@NotNull SocketMessage socketMessage, @NotNull OnMessage onMessageListener, boolean closeAfterWork) {
        m_constructor(socketMessage, onMessageListener, closeAfterWork);
    }

    private void m_constructor(@NotNull SocketMessage socketMessage, @NotNull OnMessage onMessageListener, boolean closeAfterWork) {
        m_socketMessage = socketMessage;
        m_onMessageListener = onMessageListener;
        m_closeAfterWork = closeAfterWork;

        m_messageId = m_messageGlobalId++;

        m_socketMessage.addOnMessageListener(this);
    }

    //endregion

    boolean message(byte[] data) {
        byte[] message_bytes = int2bytes(m_messageId);

        if (3 <= data.length && data[2] == M_GROUP_SEPARATOR) {
            for (int i = 0; i < 2; i++) {
                if (data[i] != message_bytes[i])
                    return false;
            }
        } else
            return false;

        byte[] newData = new byte[data.length - 3];
        System.arraycopy(data, 3, newData, 0, newData.length);

        boolean result = onMessage(newData);

        if (result && m_closeAfterWork)
            close();

        return result;
    }

    //region PUBLIC_METHODS

    public void send(byte[] data) throws SCException {
        byte[] message_bytes = Integer.toBinaryString(m_messageId).getBytes();
        byte[] groupSeparator = {M_GROUP_SEPARATOR};

        byte[] buffer = new byte[data.length + message_bytes.length + 1];

        System.arraycopy(message_bytes, 0, buffer, 0, message_bytes.length);
        System.arraycopy(groupSeparator, 0, buffer, message_bytes.length, 1);
        System.arraycopy(data, 0, buffer, message_bytes.length + 1, data.length);

        m_socketMessage.send(buffer);
    }

    public void close() {
        m_socketMessage.removeOnMessageListener(this);
    }

    //endregion

    //region OVERRIDE

    @Override
    public boolean onMessage(byte[] data) {
        return m_onMessageListener.onMessage(data);
    }

    //endregion

}
