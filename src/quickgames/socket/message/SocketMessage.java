package quickgames.socket.message;

import com.sun.istack.internal.NotNull;

import java.net.Socket;
import java.util.ArrayList;

public class SocketMessage extends quickgames.socket.message.Socket {

    //region PRIVATE_PROPERTIES

    private ArrayList<OnMessage> m_observers;

    //endregion

    //region CONSTRUCTOR

    public SocketMessage(@NotNull Socket socket) throws SCException {
        super(socket);
        m_observers = new ArrayList<>();
    }

    //endregion

    //region PUBLIC_METHODS

    public Message sendMessage(byte[] data, @NotNull OnMessage eventMessage) throws SCException {
        Message message = new Message(this, eventMessage, true);
        message.send(data);
        return message;
    }

    @Override
    public void close() throws SCException {
        super.close();
        m_observers.clear();
    }

    public void addOnMessageListener(OnMessage observer) {
        m_observers.add(observer);
    }

    public void removeOnMessageListener(OnMessage observer) {
        m_observers.remove(observer);
    }

    //endregion

    //region EVENTS

    protected boolean onMessage(byte[] data) {
        for (OnMessage observer : m_observers) {
            if (observer instanceof Message
                    ? ((Message) observer).message(data)
                    : observer.onMessage(data))
                return true;
        }

        return false;
    }

    protected void onClose() {

    }

    //endregion

}
