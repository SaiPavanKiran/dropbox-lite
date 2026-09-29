package org.rspk.dropbox_lite.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableWebMvc
@Configuration
@ComponentScan({"org.rspk.dropbox_lite.controller"}) /* after the class loaded in the context scanning starts for specified packages*/
public class DispatcherServlet implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        builder.addCustomConverter(
                new JacksonJsonHttpMessageConverter()
        );
    }

    @Override
    public Validator getValidator() {
        return new LocalValidatorFactoryBean();
    }

}
