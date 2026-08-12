package com.fan.mixmyfit.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fan.mixmyfit.domain.User;
import com.fan.mixmyfit.domain.repository.UserRepository;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class AuthenticatedUserFixture {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final TestRestTemplate restTemplate;
    private final UserRepository users;

    public AuthenticatedUserFixture(TestRestTemplate restTemplate, UserRepository users) {
        this.restTemplate = restTemplate;
        this.users = users;
    }

    public AuthenticatedUsers createUserPair() {
        return new AuthenticatedUsers(
                createLoggedInUser("user-a"),
                createLoggedInUser("user-b"));
    }

    public AuthenticatedUser createLoggedInUser(String namePrefix) {
        String username = namePrefix + "-" + SEQUENCE.incrementAndGet();
        String password = "Secret123!";
        ResponseEntity<String> registration = restTemplate.postForEntity(
                "/api/auth/register",
                Map.of(
                        "username", username,
                        "password", password,
                        "confirmPassword", password,
                        "nickname", username),
                String.class);
        assertThat(registration.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/auth/login",
                Map.of("username", username, "password", password),
                String.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String setCookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).startsWith("MMF_SESSION=");
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, setCookie.split(";", 2)[0]);

        User user = users.findByUsername(username).orElseThrow();
        return new AuthenticatedUser(user, headers);
    }
}
