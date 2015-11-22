package pingpong;

/**
 * Exception levée quand le système a reçu un ack incorrect. Il est alors freezé.
 * @author pfifzehirman
 */
public class SystemeEtatInstableException extends Exception {

    /**
     * Creates a new instance of <code>SystemEtatInstable</code> without detail message.
     */
    public SystemeEtatInstableException() {
    }

    /**
     * Constructs an instance of <code>SystemEtatInstable</code> with the specified detail message.
     * @param msg the detail message.
     */
    public SystemeEtatInstableException(String msg) {
        super(msg);
    }
}
