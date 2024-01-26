package pt.isec.pd.tp_pd;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pt.isec.pd.tp_pd.controllers.ClientController;
import pt.isec.pd.tp_pd.data.ClientRequest;
import pt.isec.pd.tp_pd.data.ClientResponse;
import pt.isec.pd.tp_pd.data.Event;
import pt.isec.pd.tp_pd.data.User;
import pt.isec.pd.tp_pd.utils.Alerts;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Client extends Application {
    private ClientController clientController;
    private static User user = null;
    private ClientResponse clientResponse = null;
    private ClientRequest clientRequest = null;
    private static final int TIMEOUT = 10; //seconds
    private static ObjectOutputStream oout = null;
    private static ObjectInputStream oin = null;

    public User getUser() {
        return user;
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Client.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 600);
        stage.setTitle("ARA - Attendance Registration Application");
        stage.setScene(scene);
        stage.setMinWidth(600);
        stage.setMinHeight(600);
        stage.show();

        // Get Client controller from FXML
        clientController = fxmlLoader.getController();
        clientController.setClient(this);
    }

    private void sendClientRequest(ClientRequest clientRequest) {
        clientController.setClient(this);
        // If the connection was established
        if (oout != null) {
            try {
                oout.writeObject(clientRequest);
                oout.flush();
            } catch (IOException e) {
                System.out.println("I/O Error: " + e);
            }
        } else {
            System.out.println("Connection to the server was not established.");
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Syntax: listeningTCPPort serverAddress");
            Platform.exit();
        } else {
            int listeningTCPPort;
            String serverAddress;

            listeningTCPPort = Integer.parseInt(args[0]);
            serverAddress = args[1];

            System.out.println("Waiting to connect to the server...");

            try (Socket socket = new Socket(serverAddress, listeningTCPPort)) {
                oout = new ObjectOutputStream(socket.getOutputStream());
                oin = new ObjectInputStream(socket.getInputStream());
                socket.setSoTimeout(TIMEOUT * 1000);
                System.out.println("Connection to the server is successful");
                launch();

            } catch (UnknownHostException e) {
                System.out.println("Unknown destination:\n\t" + e);
                Platform.exit();
            } catch (NumberFormatException e) {
                System.out.println("Server Port must be a positive integer.");
                Platform.exit();
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout exceeded, exiting the program" + e);
                Platform.exit();
            } catch (SocketException e) {
                System.out.println("There is no server running");
                Platform.exit();
            } catch (IOException e) {
                System.out.println("I/O exception:\n\t" + e);
                Platform.exit();
            }
        }
    }

    public boolean login(String email, String password) {
        System.out.println("Sending the request to the server: " + email + " " + password);
        clientRequest = new ClientRequest(new User(email, password), ClientRequest.Type.LOGIN);
        sendClientRequest(clientRequest);

        return receiveClientResponse();
    }

    public boolean register(String email, int user_id, String password) {
        clientRequest = new ClientRequest(new User(email, user_id, password), ClientRequest.Type.REGISTER);
        sendClientRequest(clientRequest);

        return receiveClientResponse();
    }

    public boolean edit(String email, String password) {
        clientRequest = new ClientRequest(
                new User(user.getUser_id(), email, password, user.getUser_type()),
                ClientRequest.Type.EDIT_PROFILE);
        sendClientRequest(clientRequest);

        return receiveClientResponse();
    }

    public boolean submitCode(String code) {
        if (user == null) {
            Alerts.showGeneralAlert("User is null");
            return false;
        }
        clientRequest = new ClientRequest(user, ClientRequest.Type.PARTICIPANT_REGISTER_ATTENDANCE, code);
        sendClientRequest(clientRequest);

        return receiveClientResponse();
    }

    public ArrayList<Event> loadAttendances() {
        if (user == null) {
            Alerts.showGeneralAlert("User is null");
            return null;
        }
        clientRequest = new ClientRequest(user, ClientRequest.Type.PARTICIPANT_CONSULT_ATTENDANCES);
        sendClientRequest(clientRequest);
        receiveClientResponse();
        if (clientResponse == null) {
            Alerts.showGeneralAlert("Client response is null");
        }
        return clientResponse.getAttendances();
    }

    public boolean receiveClientResponse() {
        try {
            // Receive Message
            clientResponse = null; // Clear the previous message
            clientResponse = (ClientResponse) oin.readObject(); // Get the response

            if (clientResponse == null) {
                Alerts.showGeneralAlert("Client Response is null");
                return false;
            }

            user = clientResponse.user; // always get the updated user
            if (clientResponse.user != null) {
                System.out.println("Response type: " + clientResponse.type.toString());
                if(clientResponse.type == ClientResponse.Type.ERROR){
                    Alerts.showGeneralAlert("Error");
                }
                if (clientResponse.type == null) {
                    Alerts.showGeneralAlert("Response type is null");
                    return false;
                }
                return true;
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error: " + e);
            return false;
        }
        return false;
    }

    public boolean createEvent(Event event) {
        if (user == null) {
            Alerts.showGeneralAlert("User is null");
            return false;
        }
        if (!user.getUser_type().equalsIgnoreCase("admin")) {
            Alerts.showGeneralAlert("User is not admin.");
            return false;
        }

        clientRequest = new ClientRequest(user, ClientRequest.Type.ADMIN_CREATE_EVENT, event);
        sendClientRequest(clientRequest);

        return receiveClientResponse();
    }

    public String generateCode() {
        clientRequest = new ClientRequest(user, ClientRequest.Type.ADMIN_GENERATE_ATTENDANCE_CODE);
        sendClientRequest(clientRequest);
        receiveClientResponse();

        if (clientResponse.type == ClientResponse.Type.ERROR)
            return null;

        return clientResponse.getCode();
    }

    public ArrayList<User> getEventUsers(Integer eventId) {
        clientRequest = new ClientRequest(user, ClientRequest.Type.ADMIN_CONSULT_EVENT_ATTENDANCES, eventId);

        sendClientRequest(clientRequest);
        receiveClientResponse();

        if (clientResponse.type == ClientResponse.Type.ERROR)
            return null;
        else {
            return (ArrayList<User>) clientResponse.getReponseResult();
        }
    }

    public ArrayList<Event> getEvents() {
        clientRequest = new ClientRequest(user, ClientRequest.Type.ADMIN_CONSULT_EVENTS);

        sendClientRequest(clientRequest);
        receiveClientResponse();

        if (clientResponse.type == ClientResponse.Type.ERROR)
            return null;
        else {
            return (ArrayList<Event>) clientResponse.getReponseResult();
        }
    }

    public ArrayList<User> getEventAttendances(Integer eventId) {
        clientRequest = new ClientRequest(user, ClientRequest.Type.ADMIN_CONSULT_EVENT_ATTENDANCES, eventId);

        sendClientRequest(clientRequest);
        receiveClientResponse();

        if (clientResponse.type == ClientResponse.Type.ERROR)
            return null;
        else {
            return (ArrayList<User>) clientResponse.getReponseResult();
        }
    }

    public boolean deleteParticipantAttendance(int eventID, int userID){
        clientRequest = new ClientRequest(user, ClientRequest.Type.ADMIN_DELETE_PARTICIPANT_ATTENDANCE, eventID, userID);

        sendClientRequest(clientRequest);
        return receiveClientResponse();
    }

    public boolean addParticipantAttendance(int eventID, String userEmail){
        clientRequest = new ClientRequest(user, ClientRequest.Type.ADMIN_ADD_PARTICIPANT_ATTENDANCE, eventID, userEmail);
        System.out.println(clientRequest);

        sendClientRequest(clientRequest);
        receiveClientResponse();

        return clientResponse != null && clientResponse.type != ClientResponse.Type.ERROR;
    }

    public boolean deleteEvent(int eventId) {
        clientRequest = new ClientRequest(user, ClientRequest.Type.ADMIN_DELETE_EVENT, eventId);

        sendClientRequest(clientRequest);
        return receiveClientResponse();
    }

    public Event getEvent(int eventId) {
        clientRequest = new ClientRequest(user, ClientRequest.Type.ADMIN_GET_EVENT, eventId);

        sendClientRequest(clientRequest);
        receiveClientResponse();

        if (clientResponse.type == ClientResponse.Type.ERROR)
            return null;
        else {
            return (Event) clientResponse.getReponseResult();
        }
    }

    public boolean updateEvent(Event event) {
        clientRequest = new ClientRequest(user, ClientRequest.Type.ADMIN_EDIT_EVENT, event);

        sendClientRequest(clientRequest);
        receiveClientResponse();

        if(clientResponse.type == ClientResponse.Type.ERROR) {

            return false;
        }

        return true;
        //return receiveClientResponse();
    }

    public ArrayList<Event> getEventsByUser(int userId) {
        clientRequest = new ClientRequest(user, ClientRequest.Type.ADMIN_GET_EVENTS_BY_USER, userId);

        sendClientRequest(clientRequest);
        receiveClientResponse();

        return (ArrayList<Event>) clientResponse.getReponseResult();


    }
}
