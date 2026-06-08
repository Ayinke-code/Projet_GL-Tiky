package fr.horizon.backend_api.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
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
import fr.horizon.backend_api.model.dto.StudentImportResultDto;
import fr.horizon.backend_api.model.dto.UserRefDto;
import fr.horizon.backend_api.model.enums.CategoryStatus;
import fr.horizon.backend_api.model.enums.CategoryType;
import fr.horizon.backend_api.model.enums.GroupType;
import fr.horizon.backend_api.repository.CategoryRepository;
import fr.horizon.backend_api.repository.EventRepository;
import fr.horizon.backend_api.repository.RoleRepository;
import fr.horizon.backend_api.repository.StudentGroupRepository;
import fr.horizon.backend_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentGroupService {

    private static final String PROMOTION_CATEGORY_NAME = "Promotion";
    private static final String ROLE_STUDENT_NAME = "STUDENT";

    private final StudentGroupRepository studentGroupRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventRepository eventRepository;

    @Transactional
    public StudentGroupDto create(StudentGroupCreateDto dto, String creatorEmail) {

        // Unicité du nom par type
        if (studentGroupRepository.existsByNameIgnoreCaseAndType(dto.getName().trim(), dto.getType())) {
            throw new IllegalArgumentException(
                    "Un groupe de type " + dto.getType() + " avec le nom \"" + dto.getName() + "\" existe déjà.");
        }

        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new UserNotFoundException(creatorEmail));

        Category category = resolveCategory(dto.getType(), dto.getCustomCategoryName(), dto.getCategoryId(), creator);

        Set<User> members = collectMembers(dto.getMemberIds(), dto.getType());

        StudentGroup group = StudentGroup.builder()
                .name(dto.getName())
                .type(dto.getType())
                .category(category)
                .createdBy(creator)
                .members(members)
                .year(dto.getYear())
                .build();

        // Groupe option parent (PROJECT)
        if (dto.getType() == GroupType.PROJECT && dto.getParentGroupId() != null) {
            StudentGroup parentGroup = studentGroupRepository.findById(dto.getParentGroupId())
                    .orElseThrow(() -> new EntityNotFoundException("Option group not found: " + dto.getParentGroupId()));
            group.setParentGroup(parentGroup);
        }

        // Promotion associée (OPTION)
        if (dto.getType() == GroupType.OPTION && dto.getPromotionId() != null) {
            StudentGroup promotion = studentGroupRepository.findById(dto.getPromotionId())
                    .orElseThrow(() -> new EntityNotFoundException("Promotion not found: " + dto.getPromotionId()));
            group.setPromotion(promotion);
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

    /**
     * Retourne les groupes d'un type donné dont l'étudiant connecté est membre.
     *
     * @param email email de l'étudiant
     * @param type  type de groupe (PROMOTION, OPTION, PROJECT)
     * @return liste des groupes filtrés
     */
    @Transactional(readOnly = true)
    public List<StudentGroupDto> findMyGroups(String email, GroupType type) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return studentGroupRepository.findByMemberIdAndType(student.getId(), type)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void deleteById(Long id) {
        StudentGroup group = studentGroupRepository.findById(id)
                .orElseThrow(() -> new StudentGroupNotFoundException(id));
        deleteCascade(group);
    }

    /**
     * Supprime un groupe et ses enfants en cascade :
     * PROMOTION → OPTION(s) → PROJECT(s).
     * Nettoie également la table event_project_groups à chaque niveau.
     */
    private void deleteCascade(StudentGroup group) {
        // Cascade vers les OPTION enfants si c'est une PROMOTION
        if (group.getType() == GroupType.PROMOTION) {
            studentGroupRepository.findByPromotion_Id(group.getId())
                    .forEach(this::deleteCascade);
        }
        // Cascade vers les PROJECT enfants si c'est une OPTION
        else if (group.getType() == GroupType.OPTION) {
            studentGroupRepository.findByParentGroup_Id(group.getId())
                    .forEach(this::deleteCascade);
        }

        // Retirer ce groupe de la table event_project_groups
        eventRepository.removeGroupFromAllEvents(group.getId());

        studentGroupRepository.delete(group);
    }

    @Transactional(readOnly = true)
    public StudentGroupDto findById(Long id) {
        return toDto(studentGroupRepository.findById(id)
                .orElseThrow(() -> new StudentGroupNotFoundException(id)));
    }

    @Transactional
    public StudentGroupDto update(Long id, StudentGroupCreateDto dto, String editorEmail) {

        StudentGroup group = studentGroupRepository.findById(id)
                .orElseThrow(() -> new StudentGroupNotFoundException(id));

        // Unicité du nom (exclure le groupe lui-même)
        if (studentGroupRepository.existsByNameIgnoreCaseAndTypeAndIdNot(
                dto.getName().trim(), group.getType(), id)) {
            throw new IllegalArgumentException(
                    "Un groupe de type " + group.getType() + " avec le nom \"" + dto.getName() + "\" existe déjà.");
        }

        // Mise à jour du nom et de l'année
        group.setName(dto.getName().trim());
        if (dto.getYear() != null) group.setYear(dto.getYear());

        // Catégorie
        if (dto.getCustomCategoryName() != null && !dto.getCustomCategoryName().isBlank()) {
            User editor = userRepository.findByEmail(editorEmail)
                    .orElseThrow(() -> new UserNotFoundException(editorEmail));
            group.setCategory(resolveCategory(group.getType(), dto.getCustomCategoryName(), null, editor));
        } else if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + dto.getCategoryId()));
            group.setCategory(cat);
        }

        if (dto.getMemberIds() != null) {
            updateMembers(group, dto.getMemberIds());
        }

        // Groupe parent (PROJECT)
        if (group.getType() == GroupType.PROJECT && dto.getParentGroupId() != null) {
            StudentGroup parent = studentGroupRepository.findById(dto.getParentGroupId())
                    .orElseThrow(() -> new EntityNotFoundException("Option group not found: " + dto.getParentGroupId()));
            group.setParentGroup(parent);
        }

        // Promotion associée (OPTION)
        if (group.getType() == GroupType.OPTION) {
            updatePromotion(group, dto.getPromotionId());
        }

        return toDto(studentGroupRepository.save(group));
    }

    private UserRefDto toUserRef(User u) {
        Set<String> roleNames = u.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return UserRefDto.builder()
                .id(u.getId())
                .name(u.getName())
                .roles(roleNames)
                .teacherType(u.getTeacherType())
                .build();
    }

    private StudentGroupDto toDto(StudentGroup group) {
        Set<UserRefDto> membersDto = group.getMembers().stream()
                .map(this::toUserRef)
                .collect(Collectors.toSet());

        // Pour les groupes PROJECT : les enseignants sont ceux du groupe d'option parent
        Set<UserRefDto> teachersDto;
        if (group.getType() == GroupType.PROJECT && group.getParentGroup() != null) {
            teachersDto = group.getParentGroup().getTeachers().stream()
                    .map(this::toUserRef)
                    .collect(Collectors.toSet());
        } else {
            teachersDto = group.getTeachers().stream()
                    .map(this::toUserRef)
                    .collect(Collectors.toSet());
        }

        return StudentGroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .type(group.getType())
                .categoryId(group.getCategory().getId())
                .categoryName(group.getCategory().getName())
                .parentGroupId(group.getParentGroup() != null ? group.getParentGroup().getId() : null)
                .parentGroupName(group.getParentGroup() != null ? group.getParentGroup().getName() : null)
                .promotionId(group.getPromotion() != null ? group.getPromotion().getId() : null)
                .promotionName(group.getPromotion() != null ? group.getPromotion().getName() : null)
                .year(group.getYear())
                .createdBy(toUserRef(group.getCreatedBy()))
                .members(membersDto)
                .teachers(teachersDto)
                .build();
    }

    @Transactional
    public StudentGroupDto assignTeacher(Long groupId, Long teacherId) {
        StudentGroup requestedGroup = studentGroupRepository.findById(groupId)
                .orElseThrow(() -> new StudentGroupNotFoundException(groupId));
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found: " + teacherId));

        // Pour les groupes PROJECT, les enseignants sont portés par le groupe OPTION parent
        StudentGroup targetGroup = (requestedGroup.getType() == GroupType.PROJECT && requestedGroup.getParentGroup() != null)
                ? requestedGroup.getParentGroup()
                : requestedGroup;

        targetGroup.getTeachers().add(teacher);
        studentGroupRepository.save(targetGroup);

        // requestedGroup.parentGroup == targetGroup (déjà à jour en mémoire) → pas de re-fetch nécessaire
        return toDto(requestedGroup);
    }

    @Transactional
    public StudentGroupDto unassignTeacher(Long groupId, Long teacherId) {
        StudentGroup requestedGroup = studentGroupRepository.findById(groupId)
                .orElseThrow(() -> new StudentGroupNotFoundException(groupId));

        // Pour les groupes PROJECT, les enseignants sont portés par le groupe OPTION parent
        StudentGroup targetGroup = (requestedGroup.getType() == GroupType.PROJECT && requestedGroup.getParentGroup() != null)
                ? requestedGroup.getParentGroup()
                : requestedGroup;

        targetGroup.getTeachers().removeIf(t -> t.getId().equals(teacherId));
        studentGroupRepository.save(targetGroup);

        return toDto(requestedGroup);
    }

    @Transactional
    public StudentImportResultDto importStudents(Long groupId, MultipartFile file) throws IOException {
        StudentGroup group = studentGroupRepository.findById(groupId)
                .orElseThrow(() -> new StudentGroupNotFoundException(groupId));

        Role studentRole = roleRepository.findByName(ROLE_STUDENT_NAME)
                .orElseThrow(() -> new EntityNotFoundException("Role " + ROLE_STUDENT_NAME + " not found"));

        List<String[]> rows = parseFile(file);

        int created = 0;
        int alreadyExisted = 0;
        int skipped = 0;

        for (String[] row : rows) {
            if (row.length >= 2) {
                String name  = row[0].trim();
                String email = row[1].trim().toLowerCase();
                if (!name.isEmpty() && !email.isEmpty() && email.contains("@")) {
                    StudentLookupResult result = findOrCreateStudent(name, email, studentRole);
                    if (result.existed()) alreadyExisted++; else created++;
                    group.getMembers().add(result.user());
                } else {
                    skipped++;
                }
            } else {
                skipped++;
            }
        }

        studentGroupRepository.save(group);

        return new StudentImportResultDto(created, alreadyExisted, skipped);
    }

    // ---- Helpers privés ----

    private record StudentLookupResult(User user, boolean existed) {}

    private CategoryType toCategoryType(GroupType type) {
        return switch (type) {
            case OPTION -> CategoryType.OPTION;
            case PROJECT -> CategoryType.PROJECT;
            case PROMOTION -> CategoryType.PROMOTION;
        };
    }

    private Category resolveCategory(GroupType type, String customName, Long categoryId, User owner) {
        if (customName != null && !customName.isBlank()) {
            CategoryType catType = toCategoryType(type);
            return categoryRepository.findByNameIgnoreCase(customName.trim())
                    .orElseGet(() -> categoryRepository.save(Category.builder()
                            .name(customName.trim())
                            .subject(customName.trim())
                            .categoryType(catType)
                            .status(CategoryStatus.ACTIVE)
                            .owner(owner)
                            .build()));
        }
        if (type == GroupType.PROMOTION) {
            return categoryRepository.findByNameIgnoreCase(PROMOTION_CATEGORY_NAME)
                    .orElseGet(() -> categoryRepository.save(Category.builder()
                            .name(PROMOTION_CATEGORY_NAME)
                            .subject(PROMOTION_CATEGORY_NAME)
                            .categoryType(CategoryType.PROMOTION)
                            .status(CategoryStatus.ACTIVE)
                            .owner(owner)
                            .build()));
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + categoryId));
    }

    private Set<User> collectMembers(Set<Long> memberIds, GroupType type) {
        Set<User> members = new HashSet<>();
        if (memberIds != null) {
            for (Long memberId : memberIds) {
                User student = userRepository.findById(memberId)
                        .orElseThrow(() -> new UserNotFoundException(String.valueOf(memberId)));
                if (type != GroupType.PROMOTION) {
                    List<StudentGroup> existing = studentGroupRepository.findByMemberIdAndType(memberId, type);
                    if (!existing.isEmpty()) {
                        throw new StudentAlreadyInGroupException(student.getName(), type);
                    }
                }
                members.add(student);
            }
        }
        if (type != GroupType.PROMOTION) {
            int minSize = type == GroupType.OPTION ? 24 : 2;
            if (members.size() < minSize) {
                throw new InvalidGroupSizeException(type, minSize, members.size());
            }
        }
        return members;
    }

    private void updateMembers(StudentGroup group, Set<Long> memberIds) {
        Set<User> members = new HashSet<>();
        for (Long memberId : memberIds) {
            members.add(userRepository.findById(memberId)
                    .orElseThrow(() -> new UserNotFoundException(String.valueOf(memberId))));
        }
        if (group.getType() != GroupType.PROMOTION) {
            int minSize = group.getType() == GroupType.OPTION ? 24 : 2;
            if (members.size() < minSize) {
                throw new InvalidGroupSizeException(group.getType(), minSize, members.size());
            }
        }
        group.getMembers().clear();
        group.getMembers().addAll(members);
    }

    private void updatePromotion(StudentGroup group, Long promotionId) {
        if (promotionId != null) {
            StudentGroup promo = studentGroupRepository.findById(promotionId)
                    .orElseThrow(() -> new EntityNotFoundException("Promotion not found: " + promotionId));
            group.setPromotion(promo);
        } else {
            group.setPromotion(null);
        }
    }

    private StudentLookupResult findOrCreateStudent(String name, String email, Role studentRole) {
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) return new StudentLookupResult(byEmail.get(), true);
        Optional<User> byName = userRepository.findByNameIgnoreCase(name);
        if (byName.isPresent()) return new StudentLookupResult(byName.get(), true);
        User student = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(generatePassword(name)))
                .roles(new HashSet<>(Set.of(studentRole)))
                .build();
        return new StudentLookupResult(userRepository.save(student), false);
    }

    private List<String[]> parseFile(MultipartFile file) throws IOException {
        String filename = Objects.requireNonNullElse(file.getOriginalFilename(), "").toLowerCase();
        if (filename.endsWith(".csv")) {
            return parseCsv(file);
        }
        return parseXlsx(file);
    }

    private List<String[]> parseCsv(MultipartFile file) throws IOException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> all = reader.readAll();
            // Ignore première ligne si c'est un en-tête (contient "nom" ou "name")
            if (!all.isEmpty()) {
                String first = all.get(0)[0].trim().toLowerCase();
                if (first.equals("nom") || first.equals("name") || first.equals("prénom")) {
                    return all.subList(1, all.size());
                }
            }
            return all;
        } catch (CsvException e) {
            throw new IOException("Erreur de lecture CSV : " + e.getMessage(), e);
        }
    }

    private List<String[]> parseXlsx(MultipartFile file) throws IOException {
        List<String[]> result = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) {
                    firstRow = false;
                    // Vérifie si c'est un en-tête
                    Cell firstCell = row.getCell(0);
                    if (firstCell != null) {
                        String val = firstCell.getStringCellValue().trim().toLowerCase();
                        if (val.equals("nom") || val.equals("name") || val.equals("prénom")) continue;
                    }
                }
                String name  = getCellString(row, 0);
                String email = getCellString(row, 1);
                if (!name.isEmpty() || !email.isEmpty()) {
                    result.add(new String[]{name, email});
                }
            }
        }
        return result;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default      -> "";
        };
    }

    private String generatePassword(String name) {
        String normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase();
        String[] parts = normalized.split("\\s+");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[parts.length - 1];
        }
        return parts[0] + ".etudiant";
    }
}
