package org.rspk.dropbox_lite.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.rspk.dropbox_lite.model.account.AuthenticatedUser;
import org.rspk.dropbox_lite.model.otp.GenerateOtp;
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

    public final AccountService accountService;

    @Autowired
    public AuthController(
            AccountService accountService
    ) {
        this.accountService = accountService;
    }

    @PostMapping("/refresh")
    ResponseEntity<?> refreshToken(
            HttpServletResponse res,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        String jwtToken = accountService.exchangeNewToken(authUser);
        res.addHeader("Authorization",jwtToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/otp")
    ResponseEntity<?> generateOtp(
            @Valid @RequestBody GenerateOtp generateOtp
    ) {
        return ResponseEntity.ok(accountService.generateOtp(generateOtp));
    }

}
