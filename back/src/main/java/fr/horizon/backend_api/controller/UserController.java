package fr.horizon.backend_api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.horizon.backend_api.model.dto.UserDto;
import fr.horizon.backend_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.repository.UserRepository;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/debug/roles")
    public List<Map<String, Object>> debugRoles() {
        return userRepository.findAll().stream()
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("name", u.getName());
                    map.put("email", u.getEmail());
                    map.put("role", u.getRole() != null ? u.getRole().getName() : "NULL");
                    return map;
                })
                .toList();
    }

    @GetMapping("/teachers")
    public List<User> getAllTeachers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "TEACHER".equalsIgnoreCase(u.getRole().getName()))
                .toList();
    }

    @PostMapping("/add")
    public User createUser(@Valid @RequestBody UserDto dto) {
        return userService.createUser(dto);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id){
        userRepository.deleteById(id);
    }
}
