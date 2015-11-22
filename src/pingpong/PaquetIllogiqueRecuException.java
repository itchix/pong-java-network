/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pingpong;

/**
 *
 * @author pfifzehirman
 */
public class PaquetIllogiqueRecuException extends Exception {

    /**
     * Creates a new instance of <code>PaquetIllogiqueRecuException</code> without detail message.
     */
    public PaquetIllogiqueRecuException() {
    }

    /**
     * Constructs an instance of <code>PaquetIllogiqueRecuException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public PaquetIllogiqueRecuException(String msg) {
        super(msg);
    }
}
