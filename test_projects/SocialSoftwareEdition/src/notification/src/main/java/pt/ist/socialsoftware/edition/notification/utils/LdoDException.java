package pt.ist.socialsoftware.edition.notification.utils;

import java.io.Serializable;

public class LdoDException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String message;

    public LdoDException() {
        this.message = "";
    }

    public LdoDException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

}
