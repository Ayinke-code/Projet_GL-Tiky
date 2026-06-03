package fr.horizon.backend_api.service;

import fr.horizon.backend_api.model.Availability;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.AvailabilityDTO;
import fr.horizon.backend_api.repository.AvailabilityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final UserService userService;

    public List<AvailabilityDTO> findByUserEmail(String email) {
        User user = userService.findByEmail(email);
        return availabilityRepository.findByUserId(user.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AvailabilityDTO createAvailability(AvailabilityDTO dto, String email) {
        if (dto == null || dto.getDate() == null || dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new IllegalArgumentException("Date, heure de début et heure de fin sont requises");
        }

        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new IllegalArgumentException("L'heure de fin doit être après l'heure de début");
        }

        User user = userService.findByEmail(email);

        Availability availability = Availability.builder()
                .date(dto.getDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .user(user)
                .build();

        return toDto(availabilityRepository.save(availability));
    }

    @Transactional
    public void deleteAvailability(Long id, String email) {
        User user = userService.findByEmail(email);

        Availability availability = availabilityRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Disponibilité introuvable ou non autorisée"));

        availabilityRepository.delete(availability);
    }

    private AvailabilityDTO toDto(Availability availability) {
        return AvailabilityDTO.builder()
                .id(availability.getId())
                .date(availability.getDate())
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .build();
    }
}
