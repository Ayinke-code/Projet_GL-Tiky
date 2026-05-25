package fr.horizon.backend_api.exception;

import fr.horizon.backend_api.model.enums.GroupType;

public class StudentAlreadyInGroupException extends RuntimeException {

    public StudentAlreadyInGroupException(String studentName, GroupType type) {
        super("L'étudiant \"" + studentName + "\" est déjà dans un groupe de type " + type.name());
    }
}
