package com.cowork.booking.user.service;

import com.cowork.booking.common.AppConstants.Audit;
import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Messages;
import com.cowork.booking.common.AppConstants.Security;
import com.cowork.booking.common.Audited;
import com.cowork.booking.common.AuthenticationFailedException;
import com.cowork.booking.common.BusinessRuleException;
import com.cowork.booking.user.dto.LoginRequest;
import com.cowork.booking.user.dto.RegisterRequest;
import com.cowork.booking.user.dto.TokenResponse;
import com.cowork.booking.user.dto.UserResponse;
import com.cowork.booking.user.mapper.UserMapper;
import com.cowork.booking.user.model.Role;
import com.cowork.booking.user.model.User;
import com.cowork.booking.user.repository.RoleRepository;
import com.cowork.booking.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserMapper userMapper;
    // Used when the email doesn't exist, so response time doesn't reveal which accounts exist
    private final String dummyHash;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.userMapper = userMapper;
        this.dummyHash = passwordEncoder.encode(AuthService.class.getName());
    }

    @Transactional
    @Audited(Audit.USER_REGISTER)
    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException(ErrorCodes.EMAIL_TAKEN, Messages.Auth.EMAIL_TAKEN.formatted(email),
                    Messages.Auth.EMAIL_FIELD);
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim(), defaultRole());
        return userMapper.toResponse(userRepository.save(user));
    }

    public TokenResponse login(LoginRequest request) {
        Optional<User> user = userRepository.findByEmailIgnoreCase(normalize(request.email()));
        String hash = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !matches) {
            throw new AuthenticationFailedException(ErrorCodes.INVALID_CREDENTIALS, Messages.Auth.INVALID_CREDENTIALS_DETAIL);
        }
        // Checked after the password on purpose, so it doesn't leak which accounts exist
        if (!user.get().isActive()) {
            throw new AuthenticationFailedException(ErrorCodes.ACCOUNT_DISABLED, Messages.Auth.ACCOUNT_DISABLED);
        }
        return tokenService.issue(user.get());
    }

    private Role defaultRole() {
        return roleRepository.findByNameIgnoreCase(Security.DEFAULT_ROLE).orElseThrow();
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
