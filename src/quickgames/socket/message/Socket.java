package quickgames.socket.message;

import com.sun.istack.internal.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Socket{

    //region PRIVATE_PROPERTIES

    private java.net.Socket m_socket;
    private OutputStream m_os;

    //endregion

    //region CONSTRUCTOR

    public Socket(@NotNull java.net.Socket socket) throws SCException {
        m_socket = socket;

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

    public void close() throws SCException {
        try {
            m_socket.close();
        } catch (IOException e) {
            throw new SCException(e);
        }

        onClose();
    }

    //endregion

    //region EVENTS

    protected void onSend(byte[] data) {

    }

    protected boolean onMessage(byte[] data) {
        return false;
    }

    protected void onClose() {

    }

    //endregion

}
