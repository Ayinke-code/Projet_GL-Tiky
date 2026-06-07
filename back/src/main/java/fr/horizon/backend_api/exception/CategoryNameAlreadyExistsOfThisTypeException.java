package fr.horizon.backend_api.exception;

public class CategoryNameAlreadyExistsOfThisTypeException extends RuntimeException {

    public CategoryNameAlreadyExistsOfThisTypeException(String name) {
        super("A category with name '" + name + "' already exists");
    }
}
