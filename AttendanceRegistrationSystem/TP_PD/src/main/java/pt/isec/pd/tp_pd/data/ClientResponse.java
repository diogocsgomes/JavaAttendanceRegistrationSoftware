package pt.isec.pd.tp_pd.data;

import java.io.Serializable;
import java.util.ArrayList;

public class ClientResponse implements Serializable {
    public final User user;
    public Type type = null;
    private ArrayList<Event> attendances = null;
    private String code;

    private Object reponseResult;

    public enum Type implements Serializable {
        SUCCESS,
        INVALID_USER,

        ERROR
    }

    @Override
    public String toString() {
        return "Response to user " + user.getEmail() + ": " + type.toString();
    }

    public ClientResponse(User user, Type type) {
        this.user = user;
        this.type = type;
    }

    public ClientResponse(User user) {
        this.user = user;
    }


    public ArrayList<Event> getAttendances() {
        return attendances;
    }

    public void setAttendances(ArrayList<Event> attendances) {
        this.attendances = attendances;
    }

    public String getCode() { return code; }

    public void setCode(String code) { this.code = code; }

    public Object getReponseResult() { return reponseResult; }
    public void setReponseResult(Object reponseResult) { this.reponseResult = reponseResult; }
}
