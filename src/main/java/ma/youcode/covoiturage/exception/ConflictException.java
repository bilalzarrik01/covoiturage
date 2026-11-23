package ma.youcode.covoiturage.exception;

public class ConflictException extends IllegalStateException {

    public ConflictException(String message) {
        super(message);
    }
}
