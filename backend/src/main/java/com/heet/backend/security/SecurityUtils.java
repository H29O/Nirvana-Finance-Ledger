package com.heet.backend.security;

import com.heet.backend.entity.User;
import com.heet.backend.exception.InvalidCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser();
        }
        throw new InvalidCredentialsException();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
