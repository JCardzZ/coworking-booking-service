package com.cowork.booking.user.service;

import com.cowork.booking.common.AppConstants.Messages;
import com.cowork.booking.common.ResourceNotFoundException;
import com.cowork.booking.user.dto.UserResponse;
import com.cowork.booking.user.mapper.UserMapper;
import com.cowork.booking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse findById(Long id) {
        return userRepository.findWithRoleById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(Messages.User.RESOURCE_TYPE, id,
                        Messages.User.NOT_FOUND.formatted(id)));
    }
}
