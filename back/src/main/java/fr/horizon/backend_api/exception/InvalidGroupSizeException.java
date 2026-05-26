package fr.horizon.backend_api.exception; 

import fr.horizon.backend_api.model.enums.GroupType;

public class InvalidGroupSizeException extends RuntimeException {

    public InvalidGroupSizeException(GroupType type, int min, int actual) {
        super("Un groupe de type " + type.name() + " doit contenir au minimum " + min
                + " étudiant(s). Vous en avez sélectionné " + actual + ".");
    }
}
