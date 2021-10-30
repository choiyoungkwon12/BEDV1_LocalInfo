package com.kdt.localinfo.user.entity;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertAll;

@Slf4j
class UserTest {
    private final Region region = Region.builder()
            .neighborhood("neighbor")
            .district("district")
            .city("city")
            .build();

    @Test
    @DisplayName("유저 정상 생성 테스트")
    void createBuilderTest() {
        User user = User.builder()
                .name("test")
                .nickname("testNickName")
                .email("test@mail.com")
                .password("testPassword")
                .roles(Set.of(Role.valueOf("GENERAL")))
                .region(region)
                .build();

        assertAll(
                () -> assertThat(user).isNotNull(),
                () -> assertThat(user.getName()).isEqualTo("test"),
                () -> assertThat(user.getNickname()).isEqualTo("testNickName"),
                () -> assertThat(user.getEmail()).isEqualTo("test@mail.com"),
                () -> assertThat(user.getPassword()).isEqualTo("testPassword"),
                () -> assertThat(user.getRoles()).isNotNull(),
                () -> assertThat(user.getPosts()).isNotNull(),
                () -> assertThat(user.getComments()).isNotNull()
        );
    }

    @Test
    @DisplayName("유저 생성 실패 테스트")
    void createFailureTest() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> User.builder()
                        .name("")
                        .nickname("")
                        .email("test.com")
                        .password("")
                        .roles(Set.of(Role.valueOf("WRONG")))
                        .region(region)
                        .build());
    }
}
