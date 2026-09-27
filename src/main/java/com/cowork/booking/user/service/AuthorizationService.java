package com.cowork.booking.user.service;

import com.cowork.booking.common.AppConstants.Caches;
import com.cowork.booking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Permissions come from the DB (cached per user), not from the token, so changes apply right away. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationService {

    private final UserRepository userRepository;

    @Cacheable(cacheNames = Caches.USER_AUTHORIZATION, key = "#userId")
    public Optional<UserAuthorization> load(Long userId) {
        return userRepository.findWithPermissionsById(userId)
                .map(user -> new UserAuthorization(user.getId(), user.getRole().getName(), user.isActive(),
                        user.getRole().permissionCodes()));
    }
}
