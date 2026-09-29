package org.rspk.dropbox_lite.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(
        basePackages = {"org.rspk.dropbox_lite.service", "org.rspk.dropbox_lite.repository" , ""},
basePackageClasses = { JWTFilter.class , MailServices.class, HibernateConfig.class , SecurityCfg.class}
)
public class RootConfig {}
