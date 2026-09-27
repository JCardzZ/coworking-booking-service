package com.cowork.booking.user.mapper;

import com.cowork.booking.user.dto.UserResponse;
import com.cowork.booking.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole().getName(),
                user.getStatus(), user.getCreatedAt());
    }
}
