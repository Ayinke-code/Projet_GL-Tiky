package fr.horizon.backend_api.controller;

import java.util.List;

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

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
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
