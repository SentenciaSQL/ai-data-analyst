package com.aidataanalyst.users.api;

import com.aidataanalyst.users.application.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('users:read')")
    public List<UserResponse> list() {
        return users.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('users:write')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return users.create(request);
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasAuthority('users:write')")
    public UserResponse update(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest request) {
        return users.update(userId, request);
    }
}
