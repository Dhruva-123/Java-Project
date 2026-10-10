package com.smartbank.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

public final class Validator {
    private Validator() {}

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("\\d{10}");
    }

    public static boolean isValidPin(String pin) {
        if (pin == null || !pin.matches("\\d{4}")) {
            return false;
        }
        char c0 = pin.charAt(0);
        char c1 = pin.charAt(1);
        char c2 = pin.charAt(2);
        char c3 = pin.charAt(3);
        if (c0 == c1 && c1 == c2 && c2 == c3) {
            return false;
        }
        if (c0 + 1 == c1 && c1 + 1 == c2 && c2 + 1 == c3) {
            return false;
        }
        if (c0 - 1 == c1 && c1 - 1 == c2 && c2 - 1 == c3) {
            return false;
        }
        return true;
    }

    public static boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0 && amount.scale() <= 2;
    }

    public static boolean isAdult(LocalDate dob) {
        if (dob == null) {
            return false;
        }
        return Period.between(dob, LocalDate.now()).getYears() >= 18;
    }

    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && name.trim().matches("^[a-zA-Z ]+$");
    }
}
