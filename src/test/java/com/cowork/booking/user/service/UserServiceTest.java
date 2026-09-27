package com.cowork.booking.user.service;

import com.cowork.booking.common.ResourceNotFoundException;
import com.cowork.booking.user.dto.UserResponse;
import com.cowork.booking.user.mapper.UserMapper;
import com.cowork.booking.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.cowork.booking.user.UserFixtures.role;
import static com.cowork.booking.user.UserFixtures.user;
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
        when(userRepository.findWithRoleById(2L)).thenReturn(Optional.of(user(2L, "ana@coworking.com", role("USER"))));

        UserResponse response = userService.findById(2L);

        assertThat(response.email()).isEqualTo("ana@coworking.com");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(UserResponse.class.getRecordComponents()).extracting("name").doesNotContain("passwordHash");
    }

    @Test
    void findByIdThrowsWhenUserDoesNotExist() {
        when(userRepository.findWithRoleById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
