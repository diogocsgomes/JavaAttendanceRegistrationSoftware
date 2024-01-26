package pt.isec.pd.tp_pd.data;

import java.io.Serializable;

public class ClientRequest implements Serializable {
    private int eventEditID = 0; // Used to edit attendances
    private int userEditID = 0; // Used to edit attendances
    private String userEditEmail = null; // Used to add attendances
    private final User user;
    private final Type type;
    private String code = null;

    public int getEventEditID() {
        return eventEditID;
    }

    public int getUserEditID() {
        return userEditID;
    }

    private Event event = null;
    private Object requestArguments;

    public ClientRequest(User user, Type type, Event event) {
        this.user = user;
        this.type = type;
        this.event = event;
    }

    public ClientRequest(User user, Type type, Object requestArguments) {
        this.user = user;
        this.type = type;
        this.requestArguments = requestArguments;
    }

    public ClientRequest(User user, Type type, int eventEditId, int userEditId){ // delete user attendance
        this.user = user;
        this.type = type;
        this.eventEditID = eventEditId;
        this.userEditID = userEditId;
    }

    public ClientRequest(User user, Type type, int eventEditId, String userEditEmail){ // Add/delete user attendance
        this.user = user;
        this.type = type;
        this.eventEditID = eventEditId;
        this.userEditEmail = userEditEmail;
    }

    public ClientRequest(User user, ClientRequest.Type type) {
        this.user = user;
        this.type = type;
    }

    public ClientRequest(User user, ClientRequest.Type type, String code) {
        this.user = user;
        this.type = type;
        this.code = code;
    }

    public Type getType() {
        return type;
    }

    public User getUser() {
        return user;
    }

    public String getCode() {
        return code;
    }

    public Event getEvent() {
        return event;
    }

    public Object getRequestArguments() { return requestArguments; }

    public enum Type implements Serializable {
        REGISTER,
        LOGIN,
        EDIT_PROFILE,
        LOGOUT,

        // Participant
        PARTICIPANT_REGISTER_ATTENDANCE,
        PARTICIPANT_CONSULT_ATTENDANCES,

        // Admin
        ADMIN_CREATE_EVENT,
        ADMIN_EDIT_EVENT,
        ADMIN_DELETE_EVENT,
        ADMIN_CONSULT_EVENTS,
        ADMIN_GENERATE_ATTENDANCE_CODE,
        ADMIN_CONSULT_EVENT_ATTENDANCES,
        ADMIN_DELETE_PARTICIPANT_ATTENDANCE,
        ADMIN_ADD_PARTICIPANT_ATTENDANCE,
        ADMIN_GET_EVENT,
        ADMIN_GET_EVENTS_BY_USER
    }

    public String getUserEditEmail() {
        return userEditEmail;
    }

    @Override
    public String toString() {
        return type.toString() + " from the user " + user.getEmail();
    }
}
