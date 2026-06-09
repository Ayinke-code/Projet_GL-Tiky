package fr.horizon.backend_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INSUFFICIENT_STORAGE) // 507
public class StorageSpaceFullException extends RuntimeException {
    public StorageSpaceFullException() {
        super("Storage space full, upload failed");
    }
}
