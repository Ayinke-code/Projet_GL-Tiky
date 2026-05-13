//package fr.horizon.backend_api.controller;
//
//import fr.horizon.backend_api.model.dto.CategoryResponseDTO;
//import fr.horizon.backend_api.model.dto.CreateCategoryDTO;
//import fr.horizon.backend_api.exception.CategoryAlreadyExistsException;
//import fr.horizon.backend_api.service.CategoryService;
package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.CategoryResponseDTO;
import fr.horizon.backend_api.model.dto.CreateCategoryDTO;
import fr.horizon.backend_api.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*", maxAge = 3600)
public class CategoryController {

	private final CategoryService categoryService;

	@GetMapping
	public ResponseEntity<List<CategoryResponseDTO>> getCategories(Authentication authentication) {
		String teacherId = authentication != null ? authentication.getName() : null;
		if (teacherId == null || teacherId.isBlank()) {
			return ResponseEntity.ok(categoryService.getAllActiveCategories());
		}

		return ResponseEntity.ok(categoryService.getAllActiveCategoriesForTeacher(teacherId));
	}

	@PostMapping
	public ResponseEntity<CategoryResponseDTO> createCategory(
			@Valid @RequestBody CreateCategoryDTO dto,
			Authentication authentication) {

		String teacherId = authentication != null ? authentication.getName() : null;
		if (teacherId == null || teacherId.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		CategoryResponseDTO createdCategory = categoryService.createCategory(dto, teacherId);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
	}

	@GetMapping("/teacher")
	public ResponseEntity<List<CategoryResponseDTO>> getCategoriesByTeacher(
			Authentication authentication) {
		String teacherEmail = authentication != null ? authentication.getName() : null;
		if (teacherEmail == null || teacherEmail.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		List<CategoryResponseDTO> categories = categoryService.getAllActiveCategoriesForTeacher(teacherEmail);
		return ResponseEntity.ok(categories);
	}

	@GetMapping("/teacher/active")
	public ResponseEntity<List<CategoryResponseDTO>> getActiveCategoriesByTeacher(
			Authentication authentication) {
		String teacherEmail = authentication != null ? authentication.getName() : null;
		if (teacherEmail == null || teacherEmail.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		List<CategoryResponseDTO> categories = categoryService.getActiveCategoriesByTeacher(teacherEmail);
		return ResponseEntity.ok(categories);
	}

	@GetMapping("/active")
	public ResponseEntity<List<CategoryResponseDTO>> getAllActiveCategories() {
		List<CategoryResponseDTO> categories = categoryService.getAllActiveCategories();
		return ResponseEntity.ok(categories);
	}

	@GetMapping("/{id}")
	public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable Long id) {
		return categoryService.getCategoryById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/search")
	public ResponseEntity<CategoryResponseDTO> getCategoryByName(@RequestParam String name) {
		return categoryService.getCategoryByName(name)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@PutMapping("/{id}")
	public ResponseEntity<CategoryResponseDTO> updateCategory(
			@PathVariable Long id,
			@Valid @RequestBody CreateCategoryDTO dto,
			Authentication authentication) {

		String requesterEmail = authentication != null ? authentication.getName() : null;
		if (requesterEmail == null || requesterEmail.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		CategoryResponseDTO updatedCategory = categoryService.updateCategory(id, dto, requesterEmail);
		return ResponseEntity.ok(updatedCategory);
	}

	@PatchMapping("/{id}/archive")
	public ResponseEntity<CategoryResponseDTO> archiveCategory(
			@PathVariable Long id,
			Authentication authentication) {
		String requesterEmail = authentication != null ? authentication.getName() : null;
		if (requesterEmail == null || requesterEmail.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return ResponseEntity.ok(categoryService.archiveCategory(id, requesterEmail));
	}

	@PatchMapping("/{id}/unarchive")
	public ResponseEntity<CategoryResponseDTO> unarchiveCategory(
			@PathVariable Long id,
			Authentication authentication) {
		String requesterEmail = authentication != null ? authentication.getName() : null;
		if (requesterEmail == null || requesterEmail.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return ResponseEntity.ok(categoryService.unarchiveCategory(id, requesterEmail));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteCategory(
			@PathVariable Long id,
			Authentication authentication) {
		String requesterEmail = authentication != null ? authentication.getName() : null;
		if (requesterEmail == null || requesterEmail.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		categoryService.deleteCategory(id, requesterEmail);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/follow")
	public ResponseEntity<Void> followCategory(
			@PathVariable Long id,
			Authentication authentication) {
		String teacherEmail = authentication != null ? authentication.getName() : null;
		if (teacherEmail == null || teacherEmail.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		categoryService.followCategory(id, teacherEmail);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{id}/follow")
	public ResponseEntity<Void> unfollowCategory(
			@PathVariable Long id,
			Authentication authentication) {
		String teacherEmail = authentication != null ? authentication.getName() : null;
		if (teacherEmail == null || teacherEmail.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		categoryService.unfollowCategory(id, teacherEmail);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/followed")
	public ResponseEntity<List<Long>> getFollowedCategoryIds(
			Authentication authentication) {
		String teacherEmail = authentication != null ? authentication.getName() : null;
		if (teacherEmail == null || teacherEmail.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return ResponseEntity.ok(categoryService.getFollowedCategoryIds(teacherEmail));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
		Map<String, String> error = new HashMap<>();
		error.put("error", "ILLEGAL_STATE");
		error.put("message", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
		Map<String, String> error = new HashMap<>();
		error.put("error", "INVALID_ARGUMENT");
		error.put("message", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

}
