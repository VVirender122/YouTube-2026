package com.app.helpers;

import java.util.regex.Pattern;

public final class DataValidations {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$");

    private static final Pattern NAME =
            Pattern.compile("^[\\p{L}][\\p{L} .'-]{0,49}$");

    private DataValidations() {
    }

    public static boolean passwordValidation(String password1, String password2) {
        return password1 != null && password1.equals(password2) && password1.length() >= 8;
    }

    public static boolean emailValidation(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches() && email.length() <= 254;
    }

    public static boolean nameValidation(String firstName, String lastName) {
        return firstName != null && lastName != null
                && NAME.matcher(firstName.trim()).matches()
                && NAME.matcher(lastName.trim()).matches();
    }
}
