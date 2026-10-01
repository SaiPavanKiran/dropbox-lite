package org.rspk.dropbox_lite.config;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import org.jspecify.annotations.Nullable;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class WebInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?> @Nullable [] getRootConfigClasses() {
        /* root context classes */
        return new Class[] { RootConfig.class };
    }

    @Override
    protected Class<?> @Nullable [] getServletConfigClasses() {
        /* dispatcher context classes - which is child to root context*/
        return new Class[] { DispatcherServlet.class };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] { "/" };
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        registration.setMultipartConfig(
                new MultipartConfigElement(
                        "/tmp",
                        -1, // unlimited file size
                        -1, // unlimited request size
                        0   // write immediately to disk
                )
        );
    }
}
