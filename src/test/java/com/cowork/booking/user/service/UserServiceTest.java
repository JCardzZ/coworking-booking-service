package com.cowork.booking.user.service;

import com.cowork.booking.common.ResourceNotFoundException;
import com.cowork.booking.user.dto.UserResponse;
import com.cowork.booking.user.mapper.UserMapper;
import com.cowork.booking.user.model.Role;
import com.cowork.booking.user.model.User;
import com.cowork.booking.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, new UserMapper());
    }

    @Test
    void findByIdReturnsUserWithoutPasswordHash() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(new User("ana@coworking.com", "hash", "Ana", Role.USER)));

        UserResponse response = userService.findById(2L);

        assertThat(response.email()).isEqualTo("ana@coworking.com");
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(UserResponse.class.getRecordComponents()).extracting("name").doesNotContain("passwordHash");
    }

    @Test
    void findByIdThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findAllMapsEachUser() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of(
                new User("admin@coworking.com", "hash", "Admin", Role.ADMIN),
                new User("ana@coworking.com", "hash", "Ana", Role.USER)), pageable, 2);
        when(userRepository.findAll(pageable)).thenReturn(page);

        assertThat(userService.findAll(pageable).getContent())
                .extracting(UserResponse::role)
                .containsExactly(Role.ADMIN, Role.USER);
    }
}
