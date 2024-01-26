package pt.isec.pd.tp_pd.data;

import java.io.Serializable;

public class User implements Serializable {
    private int user_id;
    public final String email;
    private final String password;
    private String user_type;

    public User(int user_id, String email, String password, String user_type) {
        this.user_id = user_id;
        this.email = email;
        this.password = password;
        this.user_type = user_type;
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public User(String email, int userId, String password) {
        this.email = email;
        this.user_id = userId;
        this.password = password;
    }

    public int getUser_id() {
        return user_id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getUser_type() {
        return user_type;
    }

    @Override
    public String toString() {
        return "Id: " + user_id + " | Email: " + email +
                " | Password: " + password + " | Type: " + user_type;
    }
}
