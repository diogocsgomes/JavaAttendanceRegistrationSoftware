package pt.isec.pd.tp_pd.utils;

public class InputValidator {
    public boolean isInputEmpty(String... inputs) {
        for (String input : inputs) {
            if (input.isBlank()) return true;
        }
        return false;
    }

    public boolean isEmailValid(String email) {
        return email.matches("^[a-zA-Z0-9_!#$%&’*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");
    }

    public boolean isPasswordValid(String password, String passwordConfirmation) {
        return password.equals(passwordConfirmation);
    }

    public boolean isHourValid(String hour) {
        return hour.matches("^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$");
    }
}