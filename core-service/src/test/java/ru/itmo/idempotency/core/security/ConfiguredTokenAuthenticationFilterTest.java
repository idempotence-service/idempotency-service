package ru.itmo.idempotency.core.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.itmo.idempotency.core.config.CoreProperties;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConfiguredTokenAuthenticationFilterTest {

    private CoreProperties coreProperties;
    private ConfiguredTokenAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        coreProperties = new CoreProperties();
        CoreProperties.TokenConfig operator = new CoreProperties.TokenConfig();
        operator.setToken("operator-token");
        operator.setPrincipal("operator");
        operator.setRoles(List.of("OPERATOR"));
        CoreProperties.TokenConfig admin = new CoreProperties.TokenConfig();
        admin.setToken("admin-token");
        admin.setPrincipal("admin");
        admin.setRoles(List.of("ADMIN"));
        coreProperties.getSecurity().setTokens(List.of(operator, admin));
        filter = new ConfiguredTokenAuthenticationFilter(coreProperties);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateValidOperatorToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer operator-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Assertions.assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        Assertions.assertEquals("operator", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateValidAdminToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer admin-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Assertions.assertEquals("admin", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void shouldLeaveAnonymousWhenTokenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldLeaveAnonymousWhenTokenInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer wrong-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldIgnoreNonBearerAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
