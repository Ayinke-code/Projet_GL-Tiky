package fr.horizon.backend_api.model;

import java.io.Serializable;
import java.util.Objects;

public class CategoryFollowerId implements Serializable {

    private String teacherId;
    private Long categoryId;

    public CategoryFollowerId() {
    }

    public CategoryFollowerId(String teacherId, Long categoryId) {
        this.teacherId = teacherId;
        this.categoryId = categoryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CategoryFollowerId that = (CategoryFollowerId) o;
        return Objects.equals(teacherId, that.teacherId)
                && Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teacherId, categoryId);
    }
}
