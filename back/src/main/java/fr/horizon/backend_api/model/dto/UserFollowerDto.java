package fr.horizon.backend_api.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserFollowerDto {
    private Long id;
    private String name;
    private boolean follower;
}
