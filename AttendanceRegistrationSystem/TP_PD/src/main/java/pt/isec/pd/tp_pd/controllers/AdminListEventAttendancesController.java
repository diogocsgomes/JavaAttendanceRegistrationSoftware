package pt.isec.pd.tp_pd.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import pt.isec.pd.tp_pd.Client;
import pt.isec.pd.tp_pd.data.Event;
import pt.isec.pd.tp_pd.data.User;
import pt.isec.pd.tp_pd.utils.Alerts;
import pt.isec.pd.tp_pd.utils.CsvManager;
import pt.isec.pd.tp_pd.utils.InputValidator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class AdminListEventAttendancesController {
    public VBox box;
    public TableView eventsTable;
    public TableColumn nameColumn;
    public TableColumn eventDateColumn;
    public TableView attendancesTable;
    public TableColumn emailColumn;
    public TextField userEmailField;
    public Button getCsvFileButton;

    ArrayList<Event> events;

    private Event selectedEvent;
    private User selectedAttendanceUser;

    public void initialize() {
        events = ClientController.client.getEvents();
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        eventDateColumn.setCellValueFactory(new PropertyValueFactory<>("eventDate"));
        ObservableList<Event> observableEvents = FXCollections.observableArrayList(events);
        eventsTable.setItems(observableEvents);

        eventsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {

                selectedEvent = (Event) newSelection;
                ArrayList<User> attendances = ClientController.client.getEventAttendances(selectedEvent.getEventId());

                if(attendances != null) {
                    emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
                    ObservableList<User> observableAttendances = FXCollections.observableArrayList(attendances);
                    attendancesTable.setItems(observableAttendances);
                }
            }
        });
        attendancesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedAttendanceUser = (User) newSelection;
            }

        });

    }

    private void loadUsers(){
        if(selectedEvent != null) {
        ArrayList<User> attendances = ClientController.client.getEventAttendances(selectedEvent.getEventId());

        if(attendances != null) {
            //emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
            ObservableList<User> observableAttendances = FXCollections.observableArrayList(attendances);
            attendancesTable.setItems(observableAttendances);
        }
    }
    }
    public void onCancelButtonClick() {
        tryToGoToView("admin/admin-main-view.fxml");
    }


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

    public void onDeleteAttendance() {
        if(selectedAttendanceUser != null ) {
            boolean deleted = ClientController.client.deleteParticipantAttendance(selectedEvent.getEventId(), selectedAttendanceUser.getUser_id());
            System.out.println("Deleted: " + deleted);
            loadUsers();
        } else {
            Alerts.showGeneralAlert("You must select a user to delete");
        }
    }

    public void onAddAttendance() {
        InputValidator validator = new InputValidator();

        if(selectedEvent != null){
            String userEmail = userEmailField.getText();
            if(!userEmail.isBlank()){

                if (!validator.isEmailValid(userEmail)) {
                    Alerts.showGeneralAlert("Email is invalid.");
                    return;
                }

                boolean isUserAdded = ClientController.client.addParticipantAttendance(selectedEvent.getEventId(), userEmail);

                if (!isUserAdded) {
                    Alerts.showGeneralAlert("The user hasn't been to database. Check provided email and connection to main server.");
                    return;
                }

                Alerts.informationAlert("The user has been added successfully to database.", "Add successful");

                System.out.println("Added: " + userEmail + " to database.");
                loadUsers();

            } else{
                Alerts.showGeneralAlert("You must write the email to add");
            }
        } else {
            Alerts.showGeneralAlert("You must select an event to add the user");
        }
    }

    public void onGetCsvFileButtonClick() {
        Event selEvent = (Event) eventsTable.getSelectionModel().getSelectedItem(); // selectedEvent
        if (selEvent == null) {
            Alerts.showGeneralAlert("You must choose the event first.");
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose directory");

        Window window = getCsvFileButton.getScene().getWindow();
        File chosenDirectory = directoryChooser.showDialog(window);

        if (chosenDirectory != null) {
            CsvManager csvManager = new CsvManager(chosenDirectory);

            Event event = ClientController.client.getEvent(selEvent.getEventId());
            ObservableList<User> users = attendancesTable.getItems();
            boolean isCsvSaved = csvManager.WriteToCsvFile(event, users);

            if (isCsvSaved) {
                Alerts.showCsvSuccessAlert();
            } else {
                Alerts.showCsvErrorAlert();
            }
        }
    }

    public void onCheckAttendancesButtonClick(ActionEvent actionEvent) {
        Stage popupStage = new Stage();
        popupStage.setTitle("Client Attendances");

        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> popupStage.close());

        TableView<Event> clientEventsTable = new TableView<>(); // Especificação do tipo genérico

        TableColumn<Event, String> eventNameColumn = new TableColumn<>("Event");
        eventNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        clientEventsTable.getColumns().add(eventNameColumn);

        if (selectedAttendanceUser != null) {
            ArrayList<Event> events = ClientController.client.getEventsByUser(selectedAttendanceUser.getUser_id());
            if (events != null) {
                ObservableList<Event> observableEvents = FXCollections.observableArrayList(events);
                clientEventsTable.setItems(observableEvents);
                layout.getChildren().addAll(clientEventsTable, closeButton);
            }
        }

        popupStage.setScene(new Scene(layout, 350, 350));
        popupStage.showAndWait();

    }
}
