package org.rspk.dropbox_lite.service;

import org.rspk.dropbox_lite.config.MailServices;
import org.rspk.dropbox_lite.model.account.*;
import org.rspk.dropbox_lite.model.otp.GenerateOtp;
import org.rspk.dropbox_lite.model.otp.OtpRequest;
import org.rspk.dropbox_lite.repository.OtpJpa;
import org.rspk.dropbox_lite.utils.common_functions.StringUtils;
import org.rspk.dropbox_lite.utils.exceptions.InvalidRequestException;
import org.rspk.dropbox_lite.utils.exceptions.ResourceNotFoundException;
import org.rspk.dropbox_lite.utils.exceptions.UnAuthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.rspk.dropbox_lite.repository.AccountJpa;
import org.rspk.dropbox_lite.utils.exceptions.DataNotUniqueException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AccountService implements UserDetailsService {

    private final AccountJpa accountsJpa;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final OtpJpa otpJpa;
    private final MailServices mailServices;

    @Autowired
    public AccountService(
            AccountJpa accountsJpa,
            JWTService jwtService,
            PasswordEncoder passwordEncoder,
            OtpJpa otpJpa,
            MailServices mailServices
    ) {
        this.accountsJpa = accountsJpa;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.otpJpa = otpJpa;
        this.mailServices = mailServices;
    }

    @Transactional
    public Account save(AccountReq accountDetails) {
        Optional<Account> existing = accountsJpa.findByEmail(accountDetails.email());
        if(existing.isPresent()) throw new DataNotUniqueException("email already registered");

        otpValidation(accountDetails.email(),accountDetails.otp());

        String hashedPassword = passwordEncoder.encode(accountDetails.password());
        Account account = new Account(accountDetails.email(), hashedPassword);
        return accountsJpa.save(account);
    }


    @Transactional
    public String login(AccountLogin accountLogin){
        Account account = accountsJpa.findByEmail(accountLogin.email())
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));

        // Correct the argument order (raw input goes FIRST, stored hash goes SECOND)
        if (!passwordEncoder.matches(accountLogin.password(), account.getPassword()))
            throw new ResourceNotFoundException("user not found");

        return jwtService.generateToken(account.getEmail(), account.getAccountId());
    }

    @Transactional
    public String exchangeNewToken(AuthenticatedUser authUser) {
        return jwtService.generateToken(authUser.username(), authUser.accountId());
    }

    @Transactional
    public String generateOtp(
            GenerateOtp generateOtp
    ) {
        Optional<OtpRequest> savedOtp = otpJpa.findById(generateOtp.email());
        if(savedOtp.isPresent() && savedOtp.get().getExpiredAt().isAfter(Instant.now()) )
            return "otp sent already";

        String otp = StringUtils.randomStr(8);
        mailServices.sendOtp(generateOtp.email(),otp);

        otpJpa.save(new OtpRequest(
                generateOtp.email(),
                otp,
                Instant.now().plus(10, TimeUnit.MINUTES.toChronoUnit())
        ));
        return "otp generated successfully";
    }

    private void otpValidation(
            String email,
            String otp
    ) {
        OtpRequest otpReq = otpJpa.findById(email)
                .orElseThrow(() -> new UnAuthorizedException("invalid otp"));

        if(otpReq.getExpiredAt().isBefore(Instant.now())){
            throw new InvalidRequestException("otp has been expired");
        }

        if(!Objects.equals(otpReq.getOtp(), otp))
            throw new UnAuthorizedException("invalid otp");

        otpJpa.delete(otpReq);
    }



    @Transactional
    public Account resetPassword(
            AccountReq resetPassword,
            UUID accountId
    ) {
        Account existing = accountsJpa.findByEmail(resetPassword.email())
                .orElseThrow(() -> new ResourceNotFoundException("email unregistered"));

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if(!existing.getAccountId().equals(accountId) || !encoder.matches(resetPassword.password(), existing.getPassword()))
            throw new UnAuthorizedException("invalid credentials");

        otpValidation(resetPassword.email(),resetPassword.otp());

        existing.setPassword(resetPassword.password());

        return accountsJpa.save(existing);
    }

    @Transactional
    public void delete(
            AccountReq deleteAccount,
            UUID accountId
    ) {
        Account existing = accountsJpa.findByEmail(deleteAccount.email())
                .orElseThrow(() -> new ResourceNotFoundException("email unregistered"));

        if(!existing.getAccountId().equals(accountId) || !passwordEncoder.matches(deleteAccount.password(),existing.getPassword()))
            throw new UnAuthorizedException("invalid credentials");

        otpValidation(deleteAccount.email(),deleteAccount.otp());

        accountsJpa.delete(existing);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        Account account = accountsJpa.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));

        return new CustomUserDetails(
                account.getAccountId(),
                account.getEmail(),
                account.getPassword(),
                List.of(new SimpleGrantedAuthority("USER"))
        );
    }
}