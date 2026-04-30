package fr.horizon.backend_api.controller;

import fr.horizon.backend_api.model.dto.LoginRequest;
import fr.horizon.backend_api.model.dto.LoginResponse;
import fr.horizon.backend_api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public @ResponseBody LoginResponse login(@RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

}