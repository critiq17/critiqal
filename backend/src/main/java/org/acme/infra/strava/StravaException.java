package org.acme.infra.strava;

public class StravaException extends RuntimeException{
    public StravaException(String message) { super(message); }
    public StravaException(String message, Throwable cause) { super(message, cause);}
}
