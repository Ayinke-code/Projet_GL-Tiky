package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.InvalidGroupSizeException;
import fr.horizon.backend_api.exception.StudentAlreadyInGroupException;
import fr.horizon.backend_api.exception.StudentGroupNotFoundException;
import fr.horizon.backend_api.exception.UserNotFoundException;
import fr.horizon.backend_api.model.Category;
import fr.horizon.backend_api.model.Role;
import fr.horizon.backend_api.model.StudentGroup;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.dto.StudentGroupCreateDto;
import fr.horizon.backend_api.model.dto.StudentGroupDto;
import fr.horizon.backend_api.model.dto.UserRefDto;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.model.enums.GroupType;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.StudentGroupRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentGroupService {

    private final StudentGroupRepository studentGroupRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public StudentGroupDto create(StudentGroupCreateDto dto, String creatorEmail) {

        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new UserNotFoundException(creatorEmail));

        Category category;
        String customName = dto.getCustomCategoryName();
        if (customName != null && !customName.isBlank()) {
            // Réutilise une catégorie existante du même nom ou en crée une nouvelle
            CategoryType catType = dto.getType() == GroupType.OPTION ? CategoryType.OPTION : CategoryType.PROJECT;
            category = categoryRepository.findByNameIgnoreCase(customName.trim())
                    .orElseGet(() -> categoryRepository.save(
                            Category.builder()
                                    .name(customName.trim())
                                    .subject(customName.trim())
                                    .categoryType(catType)
                                    .status(CategoryStatus.ACTIVE)
                                    .owner(creator)
                                    .build()
                    ));
        } else {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + dto.getCategoryId()));
        }

        Set<User> members = new HashSet<>();
        for (Long memberId : dto.getMemberIds()) {
            User student = userRepository.findById(memberId)
                    .orElseThrow(() -> new UserNotFoundException(String.valueOf(memberId)));

            // Règle métier : un étudiant ne peut appartenir qu'à un seul groupe par type
            List<StudentGroup> existing = studentGroupRepository.findByMemberIdAndType(memberId, dto.getType());
            if (!existing.isEmpty()) {
                throw new StudentAlreadyInGroupException(student.getName(), dto.getType());
            }

            members.add(student);
        }

        // Validation taille minimale du groupe
        int minSize = dto.getType() == GroupType.OPTION ? 24 : 2;
        if (members.size() < minSize) {
            throw new InvalidGroupSizeException(dto.getType(), minSize, members.size());
        }

        StudentGroup group = StudentGroup.builder()
                .name(dto.getName())
                .type(dto.getType())
                .category(category)
                .createdBy(creator)
                .members(members)
                .build();

        // Affectation du groupe option parent (pour les groupes PROJECT)
        if (dto.getType() == GroupType.PROJECT && dto.getParentGroupId() != null) {
            StudentGroup parentGroup = studentGroupRepository.findById(dto.getParentGroupId())
                    .orElseThrow(() -> new EntityNotFoundException("Option group not found: " + dto.getParentGroupId()));
            group.setParentGroup(parentGroup);
        }

        return toDto(studentGroupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public List<StudentGroupDto> findAll() {
        return studentGroupRepository.findAllByOrderByTypeAscNameAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentGroupDto> findByType(GroupType type) {
        return studentGroupRepository.findByTypeOrderByNameAsc(type)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void deleteById(Long id) {
        StudentGroup group = studentGroupRepository.findById(id)
                .orElseThrow(() -> new StudentGroupNotFoundException(id));
        studentGroupRepository.delete(group);
    }

    private UserRefDto toUserRef(User u) {
        Set<String> roleNames = u.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return UserRefDto.builder()
                .id(u.getId())
                .name(u.getName())
                .roles(roleNames)
                .build();
    }

    private StudentGroupDto toDto(StudentGroup group) {
        Set<UserRefDto> membersDto = group.getMembers().stream()
                .map(this::toUserRef)
                .collect(Collectors.toSet());

        return StudentGroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .type(group.getType())
                .categoryId(group.getCategory().getId())
                .categoryName(group.getCategory().getName())
                .parentGroupId(group.getParentGroup() != null ? group.getParentGroup().getId() : null)
                .parentGroupName(group.getParentGroup() != null ? group.getParentGroup().getName() : null)
                .createdBy(toUserRef(group.getCreatedBy()))
                .members(membersDto)
                .build();
    }
}
