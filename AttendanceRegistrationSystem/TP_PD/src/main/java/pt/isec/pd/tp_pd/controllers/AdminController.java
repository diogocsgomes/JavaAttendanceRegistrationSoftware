package pt.isec.pd.tp_pd.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import pt.isec.pd.tp_pd.Client;
import pt.isec.pd.tp_pd.data.Event;
import pt.isec.pd.tp_pd.data.User;
import pt.isec.pd.tp_pd.utils.Alerts;
import pt.isec.pd.tp_pd.utils.CsvManager;
import pt.isec.pd.tp_pd.utils.DateTimeFormatChecker;
import pt.isec.pd.tp_pd.utils.InputValidator;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;

public class AdminController {
    @FXML
    public Button createEventViewButton;
    public Button listEventsButton;
    public Button listClientAttendancesButton;
    public Label promptLabel;
    public TextField nameCreateEventField;
    public TextField placeCreateEventField;
    public Button createEventButton;
    public TextField nameEditEventField;
    public TextField placeEditEventField;
    public TextField startingHourEditEventField;
    public TextField endingHourEditEventField;
    public TextField codeEditEventField;
    @FXML
    public Button editEventButton;
    public Button listAttendancesEventButton;
    public VBox box;
    @FXML
    public Button logoutButton;
    public Button cancelButton;
    public DatePicker eventDatePicker;
    public TextField startingHourCreateEventField;
    public TextField endingHourCreateEventField;
    public TextField codeExpirationHourField;
    public Button getCsvFileButton;
    @FXML
    public TableView<User> eventAttendancesTable;
    @FXML
    public TableColumn email;
    @FXML
    public TableColumn user_id;
    public Label adminListEventAttendancesViewLabel;
    public Label createEventViewLabel;
    public Label adminListEventsViewLabel;
    public TableView<Event> eventsTable;
    public TableColumn<String, String> nameColumn;
    public TableColumn<String, String> dateColumn;
    public TableColumn<String, String> startHourColumn;
    public TableColumn<String, String> endHourColumn;
    public TableColumn<String, String> codeColumn;
    public Label adminEditEventViewLabel;
    public TextField codeExpirationHourEditEventField;
    public Button generateCodeButton;

    private DateTimeFormatter dateFormatter;

    private static int eventId;
    private static int codeId;

    private Event selectedEvent;

    public void initialize() {
        if (adminListEventsViewLabel != null) {
            ArrayList<Event> events = ClientController.client.getEvents();
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            dateColumn.setCellValueFactory(new PropertyValueFactory<>("eventDate"));
            startHourColumn.setCellValueFactory(new PropertyValueFactory<>("startHour"));
            endHourColumn.setCellValueFactory(new PropertyValueFactory<>("endHour"));
            codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));

            ObservableList<Event> observableEvents = FXCollections.observableArrayList(events);
            eventsTable.setItems(observableEvents);

            // enable edit button after choosing event
            eventsTable.setRowFactory( tv -> {
                TableRow<Event> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (!row.isEmpty()) {
                        editEventButton.setDisable(false);
                    }
                });
                return row ;
            });
            eventsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    selectedEvent = newSelection;
                    System.out.println("Selected event: " + selectedEvent.getName());
                }
            });
        }
        else if (adminListEventAttendancesViewLabel != null) {
            ArrayList<User> users = ClientController.client.getEventUsers(eventId);
            email.setCellValueFactory(new PropertyValueFactory<>("email"));
            user_id.setCellValueFactory(new PropertyValueFactory<>("user_id"));

            ObservableList<User> observableAttendancesList = FXCollections.observableArrayList(users);
            eventAttendancesTable.setItems(observableAttendancesList);

        }
        else if (createEventViewLabel != null) {
            setProperDataFormat();
        }
        else if (adminEditEventViewLabel != null) {
            setProperDataFormat();
            Event event = ClientController.client.getEvent(eventId); // take full event data from database

            nameEditEventField.setText(event.getName());
            placeEditEventField.setText(event.getPlace());
            LocalDate date = LocalDate.parse(event.getEventDate());
            eventDatePicker.setValue(date);
            startingHourEditEventField.setText(event.getStartHour());
            endingHourEditEventField.setText(event.getEndHour());
            codeEditEventField.setText(event.getCode());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH:mm");
            LocalTime codeExpirationTime = LocalTime.parse(event.getExpirationCodeDate(), formatter);
            codeExpirationHourEditEventField.setText(codeExpirationTime.toString());

            codeId = event.getRegistCodeId();
        }
    }

    private void setProperDataFormat() {
        dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        eventDatePicker.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(LocalDate object) {
                if (object != null) {
                    return dateFormatter.format(object);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        });
    }

    public void onCreateEventButtonClick() {
        InputValidator validator = new InputValidator();

        String eventName = nameCreateEventField.getText();
        String eventPlace = placeCreateEventField.getText();
        String startHour = startingHourCreateEventField.getText();
        String endHour = endingHourCreateEventField.getText();
        LocalDate date = eventDatePicker.getValue();
        String code = ClientController.client.generateCode();
        String expirationHour = codeExpirationHourField.getText();

        if (code == null) {
            Alerts.showValidationErrorAlert("Application had problem with generating event access code. Please try again.");
            return;
        }

        if (validator.isInputEmpty(eventName, eventPlace, startHour,
                endHour, date.toString())) {
            Alerts.showValidationErrorAlert("All fields must contain data.");
            return;
        }

        if (eventName.length() > 100) {
            Alerts.showValidationErrorAlert("Name of event must be 100 characters or less.");
            return;
        }

        if (eventPlace.length() > 100) {
            Alerts.showValidationErrorAlert("Place of event must be 100 characters or less.");
            return;
        }

        if (!validator.isHourValid(startHour)) {
            Alerts.showValidationErrorAlert("Given start hour is not in proper format.\n" +
                    " Proper format is HH:MM. In example 17:00");
            return;
        }

        if (!validator.isHourValid(endHour)) {
            Alerts.showValidationErrorAlert("Given end hour is not in proper format.\n" +
                    " Proper format is HH:MM. In example 17:00");
            return;
        }

        if (!validator.isHourValid(expirationHour)) {
            Alerts.showValidationErrorAlert("Given expiration code hour is not in proper format.\n" +
                    " Proper format is HH:MM. In example 17:00");
            return;
        }

        if (!DateTimeFormatChecker.isTimePeriodValid(startHour, expirationHour, endHour)) {
            Alerts.showValidationErrorAlert("Given expiration code hour must be between start hour end end hour.");
            return;
        }

        if (!DateTimeFormatChecker.isDateRelevant(date)) {
            Alerts.showValidationErrorAlert("You cannot create event that already happened.");
            return;
        }

        if (!DateTimeFormatChecker.isValidDateFormat(date.toString())) {
            Alerts.showValidationErrorAlert("Given date is not in proper format.\n" +
                    "Proper format is YYYY-MM-DD. In example 2023-07-28");
            return;
        }


        int creatorId = ClientController.client.getUser().getUser_id();
        String expirationDate = date + ":" + expirationHour;

        pt.isec.pd.tp_pd.data.Event event = new pt.isec.pd.tp_pd.data.Event(eventName,
                eventPlace, date.toString(), startHour, endHour, creatorId, code, expirationDate);
        boolean isCreationSuccessful = ClientController.client.createEvent(event);
        if (isCreationSuccessful) {
            Alerts.informationAlert("Your event has been created.\n" +
                    "Code to event: " + code, "Event created");

            // clear form
            nameCreateEventField.clear();
            placeCreateEventField.clear();
            startingHourCreateEventField.clear();
            endingHourCreateEventField.clear();
            eventDatePicker.setValue(null);
            codeExpirationHourField.clear();
        }
        else {
            Alerts.showValidationErrorAlert("Operation to create event failed.");
        }
    }

    public void onListEventsButtonClick() {
        tryToGoToView("admin/admin-list-events-view.fxml");
    }

    public void onListClientAttendancesButtonClick() {
        tryToGoToView("admin/admin-list-events-and-attendances-view.fxml");


    }

    public void onCreateEventViewButtonClick() {
        tryToGoToView("admin/admin-create-event-view.fxml");
    }

    public void onEditEventButtonClick() {
        eventId = eventsTable.getSelectionModel().getSelectedItem().getEventId();
        tryToGoToView("admin/admin-edit-event-view.fxml");
    }

    public void onLogoutButtonClick() {
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
            System.out.println(e.getCause().toString());
        }
    }

    public void onCancelButtonClick() {
        tryToGoToView("admin/admin-main-view.fxml");
    }

    public void onGetCsvFileButtonClick() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose directory");

        Window window = getCsvFileButton.getScene().getWindow();
        File chosenDirectory = directoryChooser.showDialog(window);

        if (chosenDirectory != null) {
            CsvManager csvManager = new CsvManager(chosenDirectory);

            Event event = new Event("Polska", "Inowroclaw", "2023-11-25", "17:00", "18:00", -1, "test6", "test7");
            ObservableList<User> users = eventAttendancesTable.getItems();
            boolean isCsvSaved = csvManager.WriteToCsvFile(event, users);

            if (isCsvSaved) {
                Alerts.showCsvSuccessAlert();
            } else {
                Alerts.showCsvErrorAlert();
            }
        }
    }

    public void onConfirmEditEventButtonClick() {
        setProperDataFormat();

        InputValidator validator = new InputValidator();

        String eventName = nameEditEventField.getText();
        String eventPlace = placeEditEventField.getText();
        String startHour = startingHourEditEventField.getText();
        String endHour = endingHourEditEventField.getText();
        LocalDate date = eventDatePicker.getValue();
        String code = codeEditEventField.getText();
        String expirationHour = codeExpirationHourEditEventField.getText();

        if (validator.isInputEmpty(eventName, eventPlace, startHour,
                endHour, date.toString())) {
            Alerts.showValidationErrorAlert("All fields must contain data.");
            return;
        }

        if (eventName.length() > 100) {
            Alerts.showValidationErrorAlert("Name of event must be 100 characters or less.");
            return;
        }

        if (eventPlace.length() > 100) {
            Alerts.showValidationErrorAlert("Place of event must be 100 characters or less.");
            return;
        }

        if (!validator.isHourValid(startHour)) {
            Alerts.showValidationErrorAlert("Given start hour is not in proper format.\n" +
                    " Proper format is HH:MM. In example 17:00");
            return;
        }

        if (!validator.isHourValid(endHour)) {
            Alerts.showValidationErrorAlert("Given end hour is not in proper format.\n" +
                    " Proper format is HH:MM. In example 17:00");
            return;
        }

        if (!validator.isHourValid(expirationHour)) {
            Alerts.showValidationErrorAlert("Given expiration code hour is not in proper format.\n" +
                    " Proper format is HH:MM. In example 17:00");
            return;
        }

        if (!DateTimeFormatChecker.isTimePeriodValid(startHour, expirationHour, endHour)) {
            Alerts.showValidationErrorAlert("Given expiration code hour must be between start hour end end hour.");
            return;
        }

        if (!DateTimeFormatChecker.isValidDateFormat(date.toString())) {
            Alerts.showValidationErrorAlert("Given date is not in proper format.\n" +
                    "Proper format is YYYY-MM-DD. In example 2023-07-28");
            return;
        }

        if (!DateTimeFormatChecker.isDateRelevant(date)) {
            Alerts.showValidationErrorAlert("You cannot edit event that already happened.");
            return;
        }

        int creatorId = ClientController.client.getUser().getUser_id();

        Event event = new Event(eventId, eventName, eventPlace,date.toString(), startHour, endHour,creatorId, codeId, code);


        event.setExpirationCodeDate(date + ":"  + expirationHour);

        boolean isEventUpdated = ClientController.client.updateEvent(event);
        if(!isEventUpdated)
            Alerts.showValidationErrorAlert("Operation to edit event failed.");

/*
        if (isEventUpdated) {
            Alerts.informationAlert("Your event has been succesfully updated.",
                    "Event updated");
        }


        else {
            Alerts.showValidationErrorAlert("Operation to edit event failed.");
        }

 */

    }

    public void onGenerateCodeButtonClick() {
        String code = ClientController.client.generateCode();
        codeEditEventField.setText(code);
    }

    public void onDeleteEvent() {
        if (selectedEvent != null) {
            boolean isDeleted = ClientController.client.deleteEvent(selectedEvent.getEventId());
            if (isDeleted) {
                //Alerts.informationAlert("Event has been deleted.", "Event deleted");
                initialize();
            }
            else {
                Alerts.showValidationErrorAlert("Operation to delete event failed.");
            }
        }
        else {
            Alerts.showValidationErrorAlert("You must select event to delete.");
        }
    }

    public void onReturnButtonClick(ActionEvent actionEvent) {
        tryToGoToView("admin/admin-list-events-view.fxml");
    }
}
