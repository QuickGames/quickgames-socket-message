package quickgames.socket.message;

public class SCException extends Exception{

    public SCException(String message) {
        super(message);
    }

    public SCException(Throwable cause) {
        super(cause.getCause());
    }
}
