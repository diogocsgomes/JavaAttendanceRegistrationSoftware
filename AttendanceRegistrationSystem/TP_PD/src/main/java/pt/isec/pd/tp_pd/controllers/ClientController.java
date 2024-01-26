package pt.isec.pd.tp_pd.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import pt.isec.pd.tp_pd.utils.Alerts;
import pt.isec.pd.tp_pd.Client;
import pt.isec.pd.tp_pd.utils.InputValidator;

import java.io.IOException;
import java.util.Objects;

public class ClientController {
    @FXML
    public TextField idRegisterField;
    final InputValidator validator = new InputValidator();
    @FXML
    public Button loginButton;
    @FXML
    public Button submitButton;
    @FXML
    public TextField codeField;
    @FXML
    public TextField emailRegisterField;
    @FXML
    public PasswordField passwordRegisterField;
    @FXML
    public PasswordField confirmPasswordRegisterField;
    @FXML
    public Button confirmRegisteringButton;
    static Client client = null;
    @FXML
    public Button registerButton;
    @FXML
    public Button editProfileViewButton;
    @FXML
    public Button registerAttendanceViewButton;
    @FXML
    public Button listAttendancesViewButton;
    @FXML
    public Button confirmEditButton;
    @FXML
    public Button cancelButton;
    @FXML
    public Button logoutButton;
    @FXML
    public Button cancelRegistrationButton;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    public VBox box;

    @FXML
    private void initialize() {
        if (confirmEditButton != null) { // checks if user entered client-edit-prifile-view
            emailRegisterField.setText(client.getUser().getEmail());
            passwordRegisterField.setText(client.getUser().getPassword());
        }
    }

    public void setClient(Client client) {
        ClientController.client = client;
    }

    @FXML
    private void onLoginButtonClick() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (client.login(email, password)) {
            loginButton.setDisable(true);
            VBox pane;

            try {
                if (Objects.equals(client.getUser().getUser_type().toLowerCase(), "admin"))
                    pane = FXMLLoader.load(Objects.requireNonNull(Client.class.getResource("admin/admin-main-view.fxml")));
                else
                    pane = FXMLLoader.load(Objects.requireNonNull(Client.class.getResource("client/participant-main-view.fxml")));

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            box.getChildren().clear();
            box.getChildren().add(pane);
        } else {
            Alerts.showGeneralAlert("That email/password combination doesn't exist");
        }
    }

    @FXML
    public void onSubmitButtonClick() {
        String code = codeField.getText();

        if (client.submitCode(code)) {
            submitButton.setDisable(true);
            tryToGoToView("client/participant-main-view.fxml");
        } else {
            Alerts.showGeneralAlert("Submit failed");
        }
    }

    @FXML
    public void onRegisterButtonClick() {
        tryToGoToView("register-view.fxml");
    }

    @FXML
    public void onConfirmRegisteringButtonClick() {
        String email = emailRegisterField.getText();
        int user_id;
        try{
            user_id = Integer.parseInt(idRegisterField.getText());
        } catch (NumberFormatException e) {
            Alerts.showValidationErrorAlert("The identification number must be a positive integer");
            return;
        }
        String password = passwordRegisterField.getText();
        String confirmPassword = confirmPasswordRegisterField.getText();

        if (!password.equals(confirmPassword)) {
            Alerts.showGeneralAlert("The passwords are not equal");
        } else {
            if (validator.isEmailValid(email)) {
                if (client.register(email, user_id, password)) {
                    confirmRegisteringButton.setDisable(true);
                    tryToGoToView("client/participant-main-view.fxml");
                } else {
                    Alerts.showGeneralAlert("Registering failed make sure that is really your student number");
                }
            } else {
                Alerts.showGeneralAlert("The email is not valid! Format: example@domain.com");
            }
        }
    }

    public void onEditProfileViewButtonClick() {
        tryToGoToView("client/participant-edit-profile-view.fxml");
    }

    public void onRegisterAttendanceViewButtonClick() {
        tryToGoToView("client/participant-register-attendance-view.fxml");
    }

    public void onListAttendancesViewButtonClick() {
        tryToGoToView("client/participant-list-attendances-view.fxml");
    }

    @FXML
    public void onConfirmEditButtonClick() {
        String email = emailRegisterField.getText();
        String password = passwordRegisterField.getText();
        String passwordConfirmation = confirmPasswordRegisterField.getText();

        if (validator.isInputEmpty(
                email,
                password,
                passwordConfirmation)) {
            Alerts.showValidationErrorAlert("Input fields cannot be empty.");
            return;
        }

        if (!validator.isEmailValid(email)) {
            Alerts.showValidationErrorAlert("Email field is not valid.");
            return;
        }

        if (!validator.isPasswordValid(password, passwordConfirmation)) {
            Alerts.showValidationErrorAlert("Password and Confirm password filed do not match.");
            return;
        }

        if (client.edit(email, password)) {
            confirmEditButton.setDisable(true);
            tryToGoToView("client/participant-main-view.fxml");
        } else {
            Alerts.showGeneralAlert("Editing profile failed");
        }
    }

    @FXML
    public void onCancelButtonClick() {
        tryToGoToView("client/participant-main-view.fxml");
    }


    @FXML
    public void onLogoutButtonClick() {
        tryToGoToView("login-view.fxml");
    }

    public void onCancelRegistrationButtonClick() {
        tryToGoToView("login-view.fxml");
    }

    // Utils functions
    @FXML
    private void tryToGoToView(String viewName) {
        try {
            VBox pane = FXMLLoader.load(Objects.requireNonNull(Client.class.getResource(viewName)));
            box.getChildren().clear();
            box.getChildren().add(pane);
        } catch (IOException e) {
            Alerts.showGeneralAlert("Error changing views");
            System.out.println(e);
        }
    }
}


