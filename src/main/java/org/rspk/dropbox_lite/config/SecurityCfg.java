package org.rspk.dropbox_lite.config;

import jakarta.annotation.PostConstruct;
import org.rspk.dropbox_lite.service.AccountService;
import org.rspk.dropbox_lite.utils.logs.CommonLogging;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityCfg {

    @Bean /*Defines what security should actually happen i.e the actual configuration*/
    public SecurityFilterChain securityFilterChain(
            JWTFilter jwtFilter,
            HttpSecurity httpSecurity
    ){
        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable) /*we have disable checking csrf token -- for now */
                .authorizeHttpRequests( auth ->
                auth
                        /* for these request the spring security doesn't check for authorization */
                        .requestMatchers(HttpMethod.POST,"/account/login", "/account/create", "/auth/otp").permitAll()
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()
                        /* for any other request, check authorization before permitting */
                        .anyRequest().authenticated()
        ).httpBasic(Customizer.withDefaults())
                .logout( httpSecurityLogoutConfigurer ->
                        httpSecurityLogoutConfigurer
                                .clearAuthentication(true)
                )
                .addFilterBefore(jwtFilter,UsernamePasswordAuthenticationFilter.class)
                .build();
    }


    @Bean /* This is the component that actually performs the authentication. */
    public AuthenticationProvider authenticationProvider(
            AccountService accountService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(accountService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }



    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*defines which authentication provider should be used like
    an application can have multiple application provider one for JWT , another for OAUTH etc*/
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) {
        return authenticationConfiguration.getAuthenticationManager();
    }

}
