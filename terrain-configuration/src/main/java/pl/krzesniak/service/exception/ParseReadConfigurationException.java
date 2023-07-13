package pl.krzesniak.service.exception;

import java.io.IOException;

public class ParseReadConfigurationException extends RuntimeException {
    public ParseReadConfigurationException(String message) {
        super(message);
    }
}
