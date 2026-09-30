package org.rspk.dropbox_lite.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.rspk.dropbox_lite.model.account.*;
import org.rspk.dropbox_lite.model.common.TemporalRes;
import org.rspk.dropbox_lite.service.JWTService;
import org.rspk.dropbox_lite.utils.exceptions.SomethingWentWrongException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.rspk.dropbox_lite.service.AccountService;

import java.util.UUID;

@RestController
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;
    private final JWTService jwtService;

    @Autowired
    public AccountController(
            AccountService accountService,
            JWTService jwtService
    ) {
        this.accountService = accountService;
        this.jwtService = jwtService;
    }

    /*Very minimal account set -- will improve later */
    @PostMapping("/create")
    ResponseEntity<?> createAccount(
            @Valid @RequestBody AccountReq accountDetails
    ) {
        Account account = accountService.save(accountDetails);
        AccountResponse accountResponse = new AccountResponse(
                account.getEmail(),
                new TemporalRes(
                        account.getCreatedAt(),
                        account.getUpdatedAt()
                )
        );
        return ResponseEntity.ok(accountResponse);
    }

    @PostMapping("/login")
    ResponseEntity<?> AccountLogin(
            @Valid @RequestBody AccountLogin accountLogin,
            HttpServletResponse res
    ) {
        Account account = accountService.login(accountLogin);
        String jwtToken = jwtService.generateToken(account.getEmail(), account.getAccountId());

        res.addHeader("Authorization", "Bearer " + jwtToken);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset")
    ResponseEntity<?> resetPassword(
            @NotNull @NotBlank @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody AccountReq resetPassword,
            Authentication authentication
    ){
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        Account account = accountService.resetPassword(resetPassword,authUser.accountId());
        AccountResponse accountResponse = new AccountResponse(
                account.getEmail(),
                new TemporalRes(
                        account.getCreatedAt(),
                        account.getUpdatedAt()
                )
        );
        return ResponseEntity.ok(accountResponse);
    }

    @DeleteMapping
    ResponseEntity<?> deleteAccount(
            @NotBlank(message = "email can't be blank")
            @Email(message = "not a valid email")
            @RequestParam("email")
            String email,
            @NotNull(message = "password can't be null")
            @Size(min = 8, max = 30,
                    message = "password must be between 8 and 30 characters")
            @RequestParam("password")
            String password,
            @NotBlank @Size(min = 8,max = 8,message = "otp must be 8 chars")
            @RequestParam("otp")
            String otp,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        AccountReq accountDetails = new AccountReq(email, password, otp);
        accountService.delete(accountDetails,authUser.accountId());
        return ResponseEntity.ok().build();
    }

}
