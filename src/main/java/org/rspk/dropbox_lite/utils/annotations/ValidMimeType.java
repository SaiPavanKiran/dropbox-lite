package org.rspk.dropbox_lite.utils.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.springframework.util.MimeTypeUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = { ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MimeTypeValidator.class)
public @interface ValidMimeType {
    String message() default "Invalid content type";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}


class MimeTypeValidator implements ConstraintValidator<ValidMimeType, String> {

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (s == null) return true;
        try {
            MimeTypeUtils.parseMimeType(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
