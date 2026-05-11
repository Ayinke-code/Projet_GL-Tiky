# Implementation Guide for Category Followers Feature

This document outlines all the code changes required to implement the category followers management feature. Each section specifies the file to modify or create, along with the code snippets to add or replace.

## Backend Changes

### 1. Update CategoryDto.java
**File:** `/Users/baffourlouis/Desktop/horizon/back/src/main/java/fr/horizon/backend_api/model/dto/CategoryDto.java`

Add followers list to the DTO:

```java
package fr.horizon.backend_api.model.dto;

import fr.horizon.backend_api.model.enums.CategoryStatus;
import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class CategoryDto {

    private Long id;
    private String name;
    private String subject;
    private CategoryStatus status;
    private String ownerName;
    private Set<UserDto> followers; // Add this line
}
```

### 2. Update UserDto.java
**File:** `/Users/baffourlouis/Desktop/horizon/back/src/main/java/fr/horizon/backend_api/model/dto/UserDto.java`

Ensure UserDto exists or create it if needed. Assuming it has basic fields:

```java
package fr.horizon.backend_api.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private Set<String> roles; // role names
}
```

### 3. Update CategoryService.java
**File:** `/Users/baffourlouis/Desktop/horizon/back/src/main/java/fr/horizon/backend_api/service/CategoryService.java`

Update the toDto method and add follower management methods:

```java
// Add imports
import fr.horizon.backend_api.model.dto.UserDto;
import java.util.stream.Collectors;

// Update toDto method
private CategoryDto toDto(Category category) {
    return CategoryDto.builder()
            .id(category.getId())
            .name(category.getName())
            .subject(category.getSubject())
            .status(category.getStatus())
            .ownerName(category.getOwner() != null ? category.getOwner().getName() : null)
            .followers(category.getFollowers().stream()
                    .map(this::userToDto)
                    .collect(Collectors.toSet())) // Add this
            .build();
}

// Add userToDto method
private UserDto userToDto(User user) {
    return UserDto.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .roles(user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet()))
            .build();
}

// Add methods for follower management
public List<UserDto> getFollowers(Long categoryId) {
    Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Category not found"));
    return category.getFollowers().stream()
            .map(this::userToDto)
            .collect(Collectors.toList());
}

public void addFollower(Long categoryId, Long userId, Long currentUserId) {
    Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Category not found"));
    
    if (!category.getOwner().getId().equals(currentUserId)) {
        throw new RuntimeException("Only category owner can add followers");
    }
    
    User teacher = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    
    boolean isTeacher = teacher.getRoles().stream()
            .anyMatch(role -> "TEACHER".equals(role.getName()));
    if (!isTeacher) {
        throw new RuntimeException("Only teachers can be added as followers");
    }
    
    if (category.getFollowers().contains(teacher)) {
        throw new RuntimeException("User is already a follower");
    }
    
    category.getFollowers().add(teacher);
    categoryRepository.save(category);
}

public void removeFollower(Long categoryId, Long userId, Long currentUserId) {
    Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Category not found"));
    
    if (!category.getOwner().getId().equals(currentUserId)) {
        throw new RuntimeException("Only category owner can remove followers");
    }
    
    User teacher = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    
    category.getFollowers().remove(teacher);
    categoryRepository.save(category);
}
```

### 4. Update CategoryController.java
**File:** `/Users/baffourlouis/Desktop/horizon/back/src/main/java/fr/horizon/backend_api/controller/CategoryController.java`

Add new endpoints:

```java
// Add imports
import fr.horizon.backend_api.model.dto.UserDto;
import org.springframework.security.core.Authentication;
import fr.horizon.backend_api.model.User;

// Add endpoints
@PreAuthorize("isAuthenticated()")
@GetMapping("/{id}/followers")
public List<UserDto> getFollowers(@PathVariable Long id) {
    return categoryService.getFollowers(id);
}

@PreAuthorize("isAuthenticated()")
@PostMapping("/{id}/followers")
public void addFollower(@PathVariable Long id, @RequestParam Long userId, Authentication auth) {
    User currentUser = (User) auth.getPrincipal();
    categoryService.addFollower(id, userId, currentUser.getId());
}

@PreAuthorize("isAuthenticated()")
@DeleteMapping("/{id}/followers/{userId}")
public void removeFollower(@PathVariable Long id, @PathVariable Long userId, Authentication auth) {
    User currentUser = (User) auth.getPrincipal();
    categoryService.removeFollower(id, userId, currentUser.getId());
}
```

### 5. Update UserRepository.java
**File:** `/Users/baffourlouis/Desktop/horizon/back/src/main/java/fr/horizon/backend_api/repository/UserRepository.java`

Add method to find teachers not following a category:

```java
// Assuming it extends JpaRepository<User, Long>
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'TEACHER' AND u NOT IN (SELECT f FROM Category c JOIN c.followers f WHERE c.id = :categoryId)")
List<User> findTeachersNotFollowingCategory(@Param("categoryId") Long categoryId);
```

### 6. Add endpoint for available teachers
In CategoryController.java, add:

```java
@PreAuthorize("isAuthenticated()")
@GetMapping("/{id}/available-teachers")
public List<UserDto> getAvailableTeachers(@PathVariable Long id) {
    return userRepository.findTeachersNotFollowingCategory(id).stream()
            .map(user -> UserDto.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                    .build())
            .collect(Collectors.toList());
}
```

## Frontend Changes

### 7. Update useCategories.js
**File:** `/Users/baffourlouis/Desktop/horizon/front/app/composables/useCategories.js`

Add methods for followers:

```javascript
// Add to the composable
const fetchFollowers = async (categoryId) => {
  return await api(`/categories/${categoryId}/followers`)
}

const addFollower = async (categoryId, userId) => {
  await api(`/categories/${categoryId}/followers?userId=${userId}`, {
    method: 'POST'
  })
}

const removeFollower = async (categoryId, userId) => {
  await api(`/categories/${categoryId}/followers/${userId}`, {
    method: 'DELETE'
  })
}

const fetchAvailableTeachers = async (categoryId) => {
  return await api(`/categories/${categoryId}/available-teachers`)
}

// Update return
return {
  categories,
  fetchCategories,
  createCategory,
  deleteCategory,
  error,
  fetchFollowers,
  addFollower,
  removeFollower,
  fetchAvailableTeachers
}
```

### 8. Create CategoryFollowers.vue component
**File:** `/Users/baffourlouis/Desktop/horizon/front/app/components/categories/CategoryFollowers.vue`

New component:

```vue
<template>
  <div class="mt-6">
    <h3 class="text-lg font-semibold mb-4">Followers</h3>
    
    <!-- Add follower section - only for owner -->
    <div v-if="isOwner" class="mb-4">
      <div class="flex gap-2">
        <select v-model="selectedTeacherId" class="flex-1 p-2 border rounded">
          <option value="">Select a teacher</option>
          <option v-for="teacher in availableTeachers" :key="teacher.id" :value="teacher.id">
            {{ teacher.name }} ({{ teacher.email }})
          </option>
        </select>
        <Button @click="addFollower" :disabled="!selectedTeacherId" label="Add" />
      </div>
    </div>
    
    <!-- Followers list -->
    <div v-if="followers.length > 0" class="space-y-2">
      <div v-for="follower in followers" :key="follower.id" class="flex justify-between items-center p-2 bg-gray-100 rounded">
        <span>{{ follower.name }} ({{ follower.email }})</span>
        <Button v-if="isOwner" @click="removeFollower(follower.id)" color="error" size="sm" label="Remove" />
      </div>
    </div>
    <p v-else class="text-gray-500">No followers yet</p>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAuth } from '~/composables/useAuth'
import Button from '~/components/ui/Button.vue'

const props = defineProps({
  category: {
    type: Object,
    required: true
  }
})

const { user } = useAuth()
const { fetchFollowers, addFollower: apiAddFollower, removeFollower: apiRemoveFollower, fetchAvailableTeachers } = useCategories()

const followers = ref([])
const availableTeachers = ref([])
const selectedTeacherId = ref('')

const isOwner = computed(() => user.value?.id === props.category.ownerId) // Assuming ownerId is added to category

const loadFollowers = async () => {
  followers.value = await fetchFollowers(props.category.id)
}

const loadAvailableTeachers = async () => {
  if (isOwner.value) {
    availableTeachers.value = await fetchAvailableTeachers(props.category.id)
  }
}

const addFollower = async () => {
  if (!selectedTeacherId.value) return
  try {
    await apiAddFollower(props.category.id, selectedTeacherId.value)
    await loadFollowers()
    await loadAvailableTeachers()
    selectedTeacherId.value = ''
  } catch (e) {
    alert('Error adding follower: ' + e.message)
  }
}

const removeFollower = async (userId) => {
  try {
    await apiRemoveFollower(props.category.id, userId)
    await loadFollowers()
    await loadAvailableTeachers()
  } catch (e) {
    alert('Error removing follower: ' + e.message)
  }
}

onMounted(() => {
  loadFollowers()
  loadAvailableTeachers()
})
</script>
```

### 9. Update categories.vue
**File:** `/Users/baffourlouis/Desktop/horizon/front/app/pages/categories.vue`

Update the detail modal to include followers:

```vue
<!-- In the template, inside the modal -->
<!-- After owner -->
<div class="text-sm text-gray-500 mb-6">
  Owner : {{ selectedCat.ownerName }}
</div>

<!-- Add followers component -->
<CategoryFollowers :category="selectedCat" />

<!-- Actions -->
```

Add import:

```javascript
import CategoryFollowers from '~/components/categories/CategoryFollowers.vue'
```

### 10. Update CategoryDto to include ownerId
In CategoryDto.java, add:

```java
private Long ownerId;
```

And in toDto:

```java
.ownerId(category.getOwner().getId())
```

## Database Changes

### 11. Ensure junction table exists
The Category entity already defines the junction table `category_followers` with unique constraint via Set.

No additional SQL needed if using JPA auto-creation.

## Notes
- Ensure the frontend category object has ownerId.
- Handle errors properly in frontend.
- Test the permissions and validations.