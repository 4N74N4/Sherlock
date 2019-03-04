/**
 * Created by IntelliJ IDEA.
 * User: Aviram Dayan
 * Date: 24/12/12
 * Time: 14:40
 *
 *<a href=mailto:avdayan@cs.bgu.ac.il>avdayan@cs.bgu.ac.il</a>
 */

/**
 * A common exception that exposes all the server internal error-codes.
 *
 * @author Aviram Dayan
 */
package com.bgu.congeor;

public class GenericException extends RuntimeException {

    private ERROR_CODE ec;

    public GenericException(ERROR_CODE ec) {
        this.ec = ec;
    }

    public GenericException(String message, ERROR_CODE ec) {
        super(message);
        this.ec = ec;
    }

    public GenericException(String message, Throwable cause, ERROR_CODE ec) {
        super(message, cause);
        this.ec = ec;
    }

    public GenericException(Throwable cause, ERROR_CODE ec) {
        super(cause);
        this.ec = ec;
    }

    public String getDisplayMessage() {
        return ec.getMessage();
    }

    public int getErrorCode() {
        return ec.getErrorCode();
    }

    public enum ERROR_CODE{
        EC_WRONG_USER_ID    (1000, "User-id doesn't exist."),
        EC_USER_EXISTS      (2000, "User already exist."),
        EC_UNKNOWN          (5000, "Unknown error.");

        private int code;
        private String message;

        private ERROR_CODE(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getErrorCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}