package com.cowork.booking.user.service;

import com.cowork.booking.common.AppConstants.Audit;
import com.cowork.booking.common.AppConstants.Caches;
import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Messages;
import com.cowork.booking.common.Audited;
import com.cowork.booking.common.BusinessRuleException;
import com.cowork.booking.common.CurrentUser;
import com.cowork.booking.common.ResourceNotFoundException;
import com.cowork.booking.common.UnprocessableOperationException;
import com.cowork.booking.user.dto.AdminCreateUserRequest;
import com.cowork.booking.user.dto.UpdateUserStatusRequest;
import com.cowork.booking.user.dto.UserResponse;
import com.cowork.booking.user.mapper.UserMapper;
import com.cowork.booking.user.model.Role;
import com.cowork.booking.user.model.User;
import com.cowork.booking.user.repository.RoleRepository;
import com.cowork.booking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** User administration; the only way to create users with a role other than USER. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    public UserResponse findById(Long userId) {
        return userMapper.toResponse(getUser(userId));
    }

    @Transactional
    @Audited(Audit.USER_CREATE)
    public UserResponse create(AdminCreateUserRequest request) {
        String email = AuthService.normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException(ErrorCodes.EMAIL_TAKEN, Messages.Auth.EMAIL_TAKEN.formatted(email),
                    Messages.Auth.EMAIL_FIELD);
        }
        String roleName = request.role().trim().toUpperCase(Locale.ROOT);
        Role role = roleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> new UnprocessableOperationException(ErrorCodes.UNKNOWN_ROLE,
                        Messages.Role.UNKNOWN.formatted(roleName)));
        User user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim(), role);
        return userMapper.toResponse(userRepository.save(user));
    }

    // Drop the cached permissions so the block applies on the next request
    @Transactional
    @Audited(Audit.USER_STATUS_UPDATE)
    @CacheEvict(cacheNames = Caches.USER_AUTHORIZATION, key = "#userId")
    public UserResponse updateStatus(Long userId, UpdateUserStatusRequest request) {
        if (CurrentUser.id().filter(userId::equals).isPresent()) {
            throw new UnprocessableOperationException(ErrorCodes.SELF_STATUS_CHANGE, Messages.Admin.SELF_STATUS_CHANGE);
        }
        User user = getUser(userId);
        user.changeStatus(request.status());
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    private User getUser(Long userId) {
        return userRepository.findWithRoleById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(Messages.User.RESOURCE_TYPE, userId,
                        Messages.User.NOT_FOUND.formatted(userId)));
    }
}
