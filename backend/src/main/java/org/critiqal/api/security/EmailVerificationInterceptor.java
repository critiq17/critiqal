package org.critiqal.api.security;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.critiqal.api.CurrentUser;
import org.critiqal.domain.shared.exception.ForbiddenException;
import org.critiqal.domain.user.service.UserService;

@Interceptor
@RequireVerifiedEmail
@Priority(Interceptor.Priority.APPLICATION)
public class EmailVerificationInterceptor {

    private final CurrentUser currentUser;
    private final UserService userService;

    public EmailVerificationInterceptor(CurrentUser currentUser, UserService userService) {
        this.currentUser = currentUser;
        this.userService = userService;
    }

    @AroundInvoke
    public Object check(InvocationContext ctx) throws Exception {
        var userId = currentUser.id();
        var user = userService.getById(userId);
        if (!user.emailVerified) {
            throw new ForbiddenException("Email verification required");
        }
        return ctx.proceed();
    }
}
