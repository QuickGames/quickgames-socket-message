package quickgames.socket.client;

import com.sun.istack.internal.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public abstract class SocketWrapper {

    //region INNER_CLASSES

    public interface OnMessage {
        boolean onMessage(byte[] data);
    }

    public static abstract class Message implements OnMessage {

        private static int m_messageGlobalId = 0;
        private static final byte M_GROUP_SEPARATOR = 0;

        //region PRIVATE_PROPERTIES

        private SocketWrapper m_socketWrapper;
        private int m_messageId;
        private boolean m_closeAfterWork;

        //endregion

        //region CONSTRUCTOR

        public Message(SocketWrapper socketWrapper) {
            m_constructor(socketWrapper, false);
        }

        public Message(SocketWrapper socketWrapper, boolean closeAfterWork) {
            m_constructor(socketWrapper, closeAfterWork);
        }

        private void m_constructor(SocketWrapper socketWrapper, boolean closeAfterWork) {
            m_socketWrapper = socketWrapper;
            m_messageId = m_messageGlobalId++;

            m_socketWrapper.addOnMessageListener(this);

            m_closeAfterWork = closeAfterWork;
        }

        //endregion

        protected boolean message(byte[] data) {
            byte[] message_bytes = Integer.toBinaryString(m_messageId).getBytes();

            if (message_bytes.length + 1 <= data.length
                    && data[message_bytes.length] == M_GROUP_SEPARATOR) {

                for (int i = 0; i < message_bytes.length; i++) {
                    if (data[i] != message_bytes[i])
                        return false;
                }

            } else
                return false;

            byte[] newData = new byte[data.length - (message_bytes.length + 1)];
            System.arraycopy(data, 2, newData, 0, newData.length);

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

            m_socketWrapper.send(buffer);
        }

        public void close() {
            m_socketWrapper.removeOnMessageListener(this);
        }

        //endregion

    }

    //endregion

    //region PRIVATE_PROPERTIES

    private Socket m_socket;

    private OutputStream m_os;

    private ArrayList<Message> m_observers;

    //endregion

    //region CONSTRUCTOR

    public SocketWrapper(@NotNull Socket socket) throws SCException {
        m_socket = socket;
        m_observers = new ArrayList<>();

        try {
            m_os = m_socket.getOutputStream();
        } catch (IOException e) {
            throw new SCException(e);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                m_bindMessage();
            }
        }).start();
    }

    //endregion

    //region PRIVATE_METHODS

    private void m_bindMessage() {
        try {
            InputStream is = m_socket.getInputStream();
            byte symbol;
            do {
                symbol = (byte) is.read();

                if (symbol >= 0) {
                    int available = is.available();
                    byte[] data = new byte[available + 1];
                    is.read(data, 1, available);
                    data[0] = symbol;

                    m_onMessage(data);
                } else
                    break;
            } while (true);
            onClose();

        } catch (IOException e) {
            onClose();
        }
    }

    private void m_onMessage(final byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (Message observer : m_observers) {
                    if (observer.message(data))
                        return;
                }

                onMessage(data);
            }
        }).start();
    }

    //endregion

    //region PUBLIC_METHODS

    public void send(byte[] data) throws SCException {
        try {
            m_os.write(data);
            m_os.flush();
            onSend(data);
        } catch (IOException e) {
            throw new SCException(e);
        }
    }

    public void addOnMessageListener(Message observer) {
        m_observers.add(observer);
    }

    public void removeOnMessageListener(Message observer) {
        m_observers.remove(observer);
    }

    public void sendMessage(byte[] data, @NotNull OnMessage eventMessage) throws SCException {
        Message message = new SingleMessage(this, eventMessage, true);
        message.send(data);
    }

    private static class SingleMessage extends Message {

        private OnMessage m_eventMessage;

        public SingleMessage(SocketWrapper socketWrapper, @NotNull OnMessage eventMessage, boolean closeAfterWork) {
            super(socketWrapper, closeAfterWork);

            m_eventMessage = eventMessage;
        }

        @Override
        public boolean onMessage(byte[] data) {
            return m_eventMessage.onMessage(data);
        }
    }

    //endregion

    //region EVENTS

    protected abstract void onSend(byte[] data);

    protected abstract boolean onMessage(byte[] data);

    protected abstract void onClose();

    public void close() throws SCException {
        try {
            m_socket.close();
        } catch (IOException e) {
            throw new SCException(e);
        }

        m_observers.clear();
        onClose();
    }

    //endregion

}
