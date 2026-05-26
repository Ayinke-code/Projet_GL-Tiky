package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.StudentGroupCreateDto;
import fr.horizon.backend_api.model.dto.StudentGroupDto;
import fr.horizon.backend_api.model.dto.StudentImportResultDto;
import fr.horizon.backend_api.model.enums.GroupType;
import fr.horizon.backend_api.service.StudentGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class StudentGroupController {

    private final StudentGroupService studentGroupService;

    @PostMapping
    @PreAuthorize("hasAuthority('TEACHER') or hasAuthority('ADMIN')")
    public ResponseEntity<StudentGroupDto> create(
            @RequestBody StudentGroupCreateDto dto,
            Authentication authentication) {
        String email = authentication.getName();
        StudentGroupDto created = studentGroupService.create(dto, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<StudentGroupDto>> findAll() {
        return ResponseEntity.ok(studentGroupService.findAll());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<StudentGroupDto>> findByType(@PathVariable GroupType type) {
        return ResponseEntity.ok(studentGroupService.findByType(type));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentGroupDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(studentGroupService.findById(id));
    }

    @PreAuthorize("hasAuthority('TEACHER') or hasAuthority('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<StudentGroupDto> update(
            @PathVariable Long id,
            @RequestBody StudentGroupCreateDto dto,
            Authentication authentication) {
        return ResponseEntity.ok(studentGroupService.update(id, dto, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public void deleteGroup(@PathVariable Long id) {
        studentGroupService.deleteById(id);
    }

    @PreAuthorize("hasAuthority('TEACHER') or hasAuthority('ADMIN')")
    @PostMapping(value = "/{id}/import-students", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentImportResultDto> importStudents(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        StudentImportResultDto result = studentGroupService.importStudents(id, file);
        return ResponseEntity.ok(result);
    }
}
