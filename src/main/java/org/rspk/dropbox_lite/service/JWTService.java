package org.rspk.dropbox_lite.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.rspk.dropbox_lite.model.account.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.Subject;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;

@Component
public class JWTService {



    @Value("${jwt.secret}")
    private String secretKey;

    private final PasswordEncoder passwordEncoder;

    public JWTService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }


    public String generateToken(String username,UUID accountId) {

        Map<String,Object> claims = new HashMap<>();

        /*JWT creates header for you, it knows that we use Hmac from the key { "alg": "HS256", "typ": "JWT" }
        * Subject, IssuedAt, expiration are the claims
        * Sign by taking encoded header and encoded payload and secret key */
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .claim("accountId",passwordEncoder.encode(accountId.toString()))
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + (30 * 60 * 1000)))
                .signWith(getKey())
                .compact();
    }


    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        /*converts the Base64 string into the original bytes.*/
        return Keys.hmacShaKeyFor(keyBytes);
        /*turns those bytes into a key that JJWT can use for HMAC signing.*/
    }


    public String extractUserName(String token) {
        if(token.isBlank()) return null;
        Claims claims = extractClaims(token);
        return claims.getSubject();
    }

    public boolean validateTokenRefresh(String token) {
        Claims claims = extractClaims(token);
        Date expiration = claims.getExpiration();
        Date now = new Date();
        long timeRemainingForExp = expiration.getTime() - now.getTime();
        long fiveMinutesMillis = 5 * 60 * 1000;
        return timeRemainingForExp <= fiveMinutesMillis;
    }

    public boolean validateToken(String token, CustomUserDetails userDetails) {
        Claims claims = extractClaims(token);
        String subject = claims.getSubject();
        String accountId = claims.get("accountId", String.class);
        Date expiration = claims.getExpiration();
        return subject.equals(userDetails.getUsername()) &&
                expiration.after(new Date(System.currentTimeMillis())) &&
                passwordEncoder.matches(userDetails.getAccountId().toString(),accountId);
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
