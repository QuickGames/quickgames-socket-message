package quickgames.socket.message;

public interface OnMessage {
    boolean onMessage(byte[] data);
}
