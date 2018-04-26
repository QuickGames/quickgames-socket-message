# quickgames-socket-message
My wrapper to socket connection

// Example of work

import quickgames.socket.message.OnMessage;
import quickgames.socket.message.SCException;
import quickgames.socket.message.SocketMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static class SWServer extends SocketMessage {

        public SWServer(Socket socket) throws SCException {
            super(socket);
        }

        @Override
        protected void onSend(byte[] data) {

        }

        @Override
        protected boolean onMessage(byte[] data) {
            try {
                send(data);
            } catch (SCException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onClose() {

        }
    }

    public static class SWClient extends SocketMessage {

        public SWClient(Socket socket) throws SCException {
            super(socket);
        }

        @Override
        protected void onSend(byte[] data) {

        }

        @Override
        protected boolean onMessage(byte[] data) {
            return false;
        }

        @Override
        protected void onClose() {

        }
    }

    public static class RunnableSocket implements Runnable {

        Socket m_socket;

        public RunnableSocket(Socket socket) {
            m_socket = socket;
        }

        @Override
        public void run() {
            try {
                new SWServer(m_socket);
            } catch (SCException e) {
                e.printStackTrace();
            }
        }
    }

    public static class ThreadSocket extends Thread {

        public ThreadSocket(final Socket socket) throws SCException {
            super(new RunnableSocket(socket));
        }
    }

    public static ServerSocket ss;
    public static int PORT = 40202;

    public static void main(String... args) {

        try {
            ss = new ServerSocket(PORT);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            new ThreadSocket(ss.accept()).start();
                        } catch (SCException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();

            Socket socket = new Socket("127.0.0.1", PORT);
            SWClient swClient = new SWClient(socket);
            swClient.sendMessage("hello world!".getBytes(), new OnMessage() {
                @Override
                public boolean onMessage(byte[] data) {
                    return true;
                }
            });
        } catch (IOException | SCException e) {
            e.printStackTrace();
        }

    }
}
