package org.critiqal.infra.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CsrfFilterTest {

    private CsrfFilter filter;
    private ContainerRequestContext ctx;

    @BeforeEach
    void setUp() {
        filter = new CsrfFilter("http://localhost:5173");
        ctx = mock(ContainerRequestContext.class);
    }

    @Test
    void post_withAllowedOrigin_passes() throws Exception {
        when(ctx.getMethod()).thenReturn("POST");
        when(ctx.getHeaderString("Origin")).thenReturn("http://localhost:5173");

        filter.filter(ctx);

        verify(ctx, never()).abortWith(any());
    }

    @Test
    void post_withWrongOrigin_returns403() throws Exception {
        when(ctx.getMethod()).thenReturn("POST");
        when(ctx.getHeaderString("Origin")).thenReturn("http://evil.example.com");

        filter.filter(ctx);

        verify(ctx).abortWith(argThat(r -> r.getStatus() == Response.Status.FORBIDDEN.getStatusCode()));
    }

    @Test
    void post_withNoOriginAndNoReferer_passes() throws Exception {
        when(ctx.getMethod()).thenReturn("POST");
        when(ctx.getHeaderString("Origin")).thenReturn(null);
        when(ctx.getHeaderString("Referer")).thenReturn(null);

        filter.filter(ctx);

        verify(ctx, never()).abortWith(any());
    }

    @Test
    void post_withNoOriginButAllowedReferer_passes() throws Exception {
        when(ctx.getMethod()).thenReturn("POST");
        when(ctx.getHeaderString("Origin")).thenReturn(null);
        when(ctx.getHeaderString("Referer")).thenReturn("http://localhost:5173/some/path");

        filter.filter(ctx);

        verify(ctx, never()).abortWith(any());
    }

    @Test
    void post_withNoOriginButBadReferer_returns403() throws Exception {
        when(ctx.getMethod()).thenReturn("POST");
        when(ctx.getHeaderString("Origin")).thenReturn(null);
        when(ctx.getHeaderString("Referer")).thenReturn("http://evil.example.com/page");

        filter.filter(ctx);

        verify(ctx).abortWith(argThat(r -> r.getStatus() == Response.Status.FORBIDDEN.getStatusCode()));
    }

    @Test
    void get_isNeverBlocked() throws Exception {
        when(ctx.getMethod()).thenReturn("GET");
        when(ctx.getHeaderString("Origin")).thenReturn("http://evil.example.com");

        filter.filter(ctx);

        verify(ctx, never()).abortWith(any());
    }

    @Test
    void post_withTelegramOrigin_passes() throws Exception {
        when(ctx.getMethod()).thenReturn("POST");
        when(ctx.getHeaderString("Origin")).thenReturn("https://web.telegram.org");

        filter.filter(ctx);

        verify(ctx, never()).abortWith(any());
    }
}
