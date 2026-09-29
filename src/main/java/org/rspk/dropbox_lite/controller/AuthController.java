package org.rspk.dropbox_lite.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.rspk.dropbox_lite.model.account.AuthenticatedUser;
import org.rspk.dropbox_lite.model.otp.GenerateOtp;
import org.rspk.dropbox_lite.service.JWTService;
import org.rspk.dropbox_lite.utils.exceptions.SomethingWentWrongException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.rspk.dropbox_lite.service.AccountService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AccountService accountService;
    private final JWTService jwtService;

    @Autowired
    public AuthController(
            AccountService accountService,
            JWTService jwtService
    ) {
        this.accountService = accountService;
        this.jwtService = jwtService;
    }

    @PostMapping("/refresh")
    ResponseEntity<?> refreshToken(
            HttpServletResponse res,
            HttpServletRequest req,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        String authHeader = req.getHeader("Authorization");
        if(jwtService.validateTokenRefresh(authHeader.substring(7))) {
            String jwtToken = jwtService.generateToken(authUser.username(), authUser.accountId());
            res.addHeader("Authorization","Bearer " + jwtToken);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/otp")
    ResponseEntity<?> generateOtp(
            @Valid @RequestBody GenerateOtp generateOtp
    ) {
        return ResponseEntity.ok(accountService.generateOtp(generateOtp));
    }

}
