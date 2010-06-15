package twetailer.http;

/**
 * Conveyer of errors reported by the Twetailer back-end through the REST API
 *
 * @author Dom Derrien
 */
public class RestException extends Exception {

    private static final long serialVersionUID = 5986025395945703632L;

    private int code = UNKNOWN_CAUSE;

    public final static int UNKNOWN_CAUSE = 0;
    public final static int MALFORMED_URL = 1;
    public final static int NOT_HTTP_URL = 2;
    public final static int BAD_PROTOCOL = 3;
    public final static int IO_EXCEPTION = 4;
    public final static int JSON_PARSING = 5;

    public RestException() {
        super("Unexpected REST error");
    }

    public RestException(String message) {
        super(message);
    }

    public RestException(String message, int code) {
        super(message);
        this.code = code;
    }

    public RestException(String message, int code, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
