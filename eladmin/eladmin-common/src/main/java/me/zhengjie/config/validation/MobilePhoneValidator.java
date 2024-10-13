package me.zhengjie.config.validation;

import me.zhengjie.annotation.validation.MobilePhone;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class MobilePhoneValidator implements ConstraintValidator<MobilePhone, String> {

    @Override
    public void initialize(MobilePhone constraintAnnotation) {

    }

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext constraintValidatorContext) {
        if (StringUtils.isNotBlank(phone)) {
            String regex = "^(?:(?:\\+|00)86)?1[3-9]\\d{9}$";
            return phone.matches(regex);
        }
        return true;
    }
}
