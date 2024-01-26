package pt.isec.pd.tp_pd.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import pt.isec.pd.tp_pd.utils.Alerts;
import pt.isec.pd.tp_pd.Client;
import pt.isec.pd.tp_pd.data.Event;
import pt.isec.pd.tp_pd.utils.CsvManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class ClientAttendancesListController {

    @FXML
    public TableView<Event> attendancesTable;
    @FXML
    public TableColumn<Event, String> nameColumn;
    @FXML
    public TableColumn<Event, String> placeColumn;
    @FXML
    public TableColumn<Event, String> dateColumn;
    @FXML
    public TableColumn<Event, String> hourColumn;

    public VBox box;
    public Button getCsvFileButton;

    private ArrayList<Event> attendances;

    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        placeColumn.setCellValueFactory(new PropertyValueFactory<>("place"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("eventDate"));
        hourColumn.setCellValueFactory(new PropertyValueFactory<>("startHour"));

        attendances = ClientController.client.loadAttendances();
        ObservableList<Event> observableAttendancesList = FXCollections.observableArrayList(attendances);
        attendancesTable.setItems(observableAttendancesList);
    }

    public void onReturnButton() {
        tryToGoToView("client/participant-main-view.fxml");
    }

    public void onGetCsvFileButtonClick() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose directory");

        Window window = getCsvFileButton.getScene().getWindow();
        File chosenDirectory = directoryChooser.showDialog(window);

        if (chosenDirectory != null) {
            CsvManager csvManager = new CsvManager(chosenDirectory);
            boolean isCsvSaved = csvManager.WriteToCsvFile(ClientController.client.getUser(), attendances);

            if (isCsvSaved) {
                Alerts.showCsvSuccessAlert();
            } else {
                Alerts.showCsvErrorAlert();
            }
        }
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
}
