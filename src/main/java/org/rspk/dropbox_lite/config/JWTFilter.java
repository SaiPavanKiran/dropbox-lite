package org.rspk.dropbox_lite.config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.rspk.dropbox_lite.model.account.AuthenticatedUser;
import org.rspk.dropbox_lite.model.account.CustomUserDetails;
import org.rspk.dropbox_lite.service.AccountService;
import org.rspk.dropbox_lite.service.JWTService;
import org.rspk.dropbox_lite.utils.exceptions.UnAuthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JWTFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final ApplicationContext context;
    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);

    public JWTFilter(
            JWTService jwtService,
            ApplicationContext context
    ) {
        this.jwtService = jwtService;
        this.context = context;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = null;
        String username = null;

        String authorization = request.getHeader("Authorization");
        if(authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
            try {
                username = jwtService.extractUserName(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    CustomUserDetails userDetails = (CustomUserDetails) context.getBean(AccountService.class).loadUserByUsername(username);
                    if (jwtService.validateToken(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = getUsernamePasswordAuthenticationToken(userDetails);

                        /*
                         *  this adds additional information about the request. For example, information such as: Remote IP address, Session ID
                         * The important thing is: This is additional request metadata. It is not what actually authenticates the user*/
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        /* Now Spring Security knows: The current request is authenticated as some user */
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (ExpiredJwtException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("jwt token has been expired");
                return;

            } catch (JwtException e) {
                logger.error("invalid jwt token - {}",token);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid jwt token");
                return;
            }
        }
        filterChain.doFilter(request,response);
    }

    private static UsernamePasswordAuthenticationToken getUsernamePasswordAuthenticationToken(CustomUserDetails userDetails) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userDetails.getAccountId(), userDetails.getUsername());
        /*You're using this class as an implementation of Spring Security's Authentication interface to represent an authenticated user.*/
        /* we are using Jwt token to authenticate so we don't need user password i.e. the credential = null
        * authorities is fixed i.e. User since our application will only have users currently */
        return new UsernamePasswordAuthenticationToken(authenticatedUser /*this is called principle*/,null, userDetails.getAuthorities());
    }
}
