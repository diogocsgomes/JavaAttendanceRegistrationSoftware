package pt.isec.pd.tp_pd.database;

import pt.isec.pd.tp_pd.MainServer;
import pt.isec.pd.tp_pd.data.Event;
import pt.isec.pd.tp_pd.data.User;
import pt.isec.pd.tp_pd.utils.DateTimeFormatChecker;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:./sqlite/DP_ProjectDB.sqlite";
    private final String pathToDB = "./sqlite/DP_ProjectDB.sqlite";
    private final MainServer mainServer;
    //private static final String URL_DE_TESTE = "jdbc:sqlite:./sqlite/DP_ProjectDB_TEST.sqlite";

    Connection conn;

    public DatabaseManager(MainServer mainServer) throws SQLException {
        this.mainServer = mainServer;
        File dbFile = new File(pathToDB);
        if(!dbFile.exists()) {
            System.out.println("Database doesn't exist, creating...");
            if (!createDatabase())
                System.out.println("Error creating database");
            else
                System.out.println("Database created with success!");
        }

        conn = DriverManager.getConnection(URL);
        System.out.println("Connection established with success!");
    }

    public long getDatabaseVersion(){
        long databaseVersion = 1L;
        String sql = "SELECT * FROM DB_versions ORDER BY version_number";

        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){
            while(rs.next()){
                long temp = rs.getLong("version_number");
                if(temp != 1L){
                    databaseVersion = temp;
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }

        return databaseVersion;
    }

    public boolean alterDbVersion() { //THIS METHOD SHOULD BE INVOKED EVERY TIME SOMETHING IS ALTERED ON THE DB
        String sql = "INSERT INTO DB_versions(date) VALUES(?)";
        int affected;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            Calendar c = Calendar.getInstance();
            pstmt.setString(1, c.getTime().toString());
            affected = pstmt.executeUpdate();

        } catch (SQLException e) {
            return false;
        }
        if(affected != 0){
            long version = getDatabaseVersion();
            MainServer.updateDatabase((int) version);
            MainServer.sendHeartBeat();
        }
        return affected != 0;
    }

    public String listUsers(){
        String sql = "SELECT * FROM users ";
        StringBuilder out = new StringBuilder();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                out.append("Id: ").append(rs.getInt("user_id")).append(" | Email: ").append(rs.getString("email")).append(" | Password: ").append(rs.getString("password")).append(" | Type: ").append(rs.getString("user_type")).append("\n");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }

        return out.toString();
    }

    public User insertUser(String email, String password, String user_type, int user_id) {
        String sqlGetUserWithSameUserId = "SELECT COUNT(*) as TOTAL from users where user_id == ?";
        String sqlVerifyEmailInDB = "SELECT COUNT(*) as TOTAL from users where email== ?";
        int numUsersWithSameEmail;
        String sql = "INSERT INTO users(email,password,user_type,user_id) VALUES(?,?,?,?)";
        int affected;

        try(PreparedStatement pstmt = conn.prepareStatement(sqlGetUserWithSameUserId)){
            pstmt.setInt(1,user_id);
            ResultSet resultSet = pstmt.executeQuery();
            int numUserWithID = resultSet.getInt("TOTAL");
            if(numUserWithID > 0)
                return null;
        }catch (SQLException e)
        {
            System.out.println("SQL Error: " + e);
            return null;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sqlVerifyEmailInDB)) {
            stmt.setString(1, email);
            ResultSet result = stmt.executeQuery();
            numUsersWithSameEmail = result.getInt("TOTAL");
            if (numUsersWithSameEmail >= 1) {
                System.out.println("Invalid email");
                return null;
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return null;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            pstmt.setString(3, user_type);
            pstmt.setInt(4, user_id);
            affected = pstmt.executeUpdate();
        } catch (Exception e) {
            return null;
        }

        if (affected == 0)
            return null;

        //System.out.println("Inserted user result = " + alterDbVersion());
        alterDbVersion();
        return login(email, password);
    }

    public boolean editUser(int user_id, String email, String password) {
        String sql = "UPDATE users SET email = ?, password = ? where user_id = ?";
        int affected; //Will determine the number of lines altered, if the function is succeded it will be more than 0

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            pstmt.setInt(3, user_id);
            affected = pstmt.executeUpdate();
        } catch (Exception e) {
            return false;
        }

        if (affected > 0) {
            //System.out.println("User edited with success = " +alterDbVersion());
            alterDbVersion();
            return true;
        }

        return false;
    }

    public Event insertEvent(
            String name, String place, String event_date, String start_hour, String end_hour,
            int creator_id, String regist_code, String expiration_date
    ) {
        Event event = null;
        String sqlInsertEvent = "INSERT INTO events(name,place,event_date,start_hour,end_hour,creator_id,code) VALUES(?,?,?,?,?,?,?)";
        String sqlGetEvent = "SELECT * from events WHERE event_id = ?";
        String sqlGetEventId = "SELECT event_id from events WHERE code == ?";
        String sqlInsertRegistCode = "INSERT INTO regist_codes(code,expiration_date,event_id) VALUES(?,?,?)";
        String sqlGetCodeId = "SELECT * FROM regist_codes WHERE event_id = ?";
        String sqlInsertCodeIdIntoEvent = "UPDATE events SET regist_code_id = ? WHERE event_id = ?";
        int codeID = 0;
        int event_id = 0;
        int affected;

        if (!DateTimeFormatChecker.isValidDateFormat(event_date)) {
            System.out.println("Wrong date format");
            return null;
        }

        if (!DateTimeFormatChecker.isValidTimeFormat(start_hour) || !DateTimeFormatChecker.isValidTimeFormat(end_hour)) {
            System.out.println("Wrong hour format");
            return null;
        }

        if (!DateTimeFormatChecker.isValidDateTimeFormat(expiration_date)) {
            System.out.println("Wrong date time format");
            return null;
        }

        if (doesCodeExist(regist_code)) {
            System.out.println("Invalid code");
            return null;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sqlInsertEvent)) {
            pstmt.setString(1, name);
            pstmt.setString(2, place);
            pstmt.setString(3, event_date);
            pstmt.setString(4, start_hour);
            pstmt.setString(5, end_hour);
            pstmt.setInt(6, creator_id);
            pstmt.setString(7, regist_code);
            affected = pstmt.executeUpdate();

        } catch (Exception e) {
            return null;
        }

        if (affected == 0) {
            return null;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sqlGetEventId)) {
            pstmt.setString(1, regist_code);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                event_id = rs.getInt("event_id");
            }
            if (event_id <= 0) {
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sqlInsertRegistCode)) {
            pstmt.setString(1, regist_code);
            pstmt.setString(2, expiration_date);
            pstmt.setInt(3, event_id);
            affected += pstmt.executeUpdate();
        } catch (Exception e) {
            return null;
        }

        if (affected <= 1) {
            return null;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sqlGetCodeId)) {
            //System.out.println("Event id is: " + event_id);
            pstmt.setInt(1, event_id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                codeID = rs.getInt("code_id");
            }
            //System.out.println("Code id is: " + codeID);

        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sqlInsertCodeIdIntoEvent)) {
            pstmt.setInt(1, codeID);
            pstmt.setInt(2, event_id);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
        }

        //System.out.println("Event added = " + alterDbVersion());
        alterDbVersion();

        try (PreparedStatement pstmt = conn.prepareStatement(sqlGetEvent)) {
            pstmt.setInt(1, event_id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println("Event " + rs.getString("name") + " was added with success!");
                event = new Event(
                        rs.getInt("event_id"),
                        rs.getString("name"),
                        rs.getString("place"),
                        rs.getString("event_date"),
                        rs.getString("start_hour"),
                        rs.getString("end_hour"),
                        rs.getInt("creator_id"),
                        rs.getInt("regist_code_id"),
                        rs.getString("code")
                );
            }

        } catch (Exception e) {
            return null;
        }

        return event;
    }

    public boolean doesCodeExist(String code) {
        String sql = "SELECT COUNT(*) as TOTAL from regist_codes where code == ?";
        int count;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();
            count = rs.getInt("TOTAL");

        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return false;
        }

        return count > 0;
    }

    public User login(String email, String password) {
        String sql = "SELECT * from users WHERE email == ? AND password == ?";

        if (email == null || password == null)
            return null;

        try (PreparedStatement pstm = conn.prepareStatement(sql)) {

            pstm.setString(1, email);
            pstm.setString(2, password);

            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                System.out.println("User with email " + rs.getString("email") + " just logged in");
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("user_type")
                );
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return null;
        }

        return null;
    }

    public boolean registerAttendence(String code, int userId) {
        String getAllAtendences = "SELECT event_id FROM attendances WHERE user_id == ?";
        String getAtendedEventsSQL = "SELECT * FROM events where event_id ==?";//gets the events that the user as registred in
        String getEventSQL = "SELECT * FROM events where code == ?";
        String getCodeSQL = "SELECT * FROM regist_codes where code_id == ?";
        String insertAttendennceSQL = "INSERT INTO attendances(event_id,user_id) VALUES (?,?)";
        String eventDate;
        String eventStartDate;
        String codeDate;
        int codeId;
        int eventId;

        LocalDateTime localDate = LocalDateTime.now();
        DateTimeFormatter dateformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH:mm");


        try (PreparedStatement pstmt = conn.prepareStatement(getAllAtendences)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next())
                try (PreparedStatement pstmt2 = conn.prepareStatement(getAtendedEventsSQL)) {
                    pstmt2.setInt(1, rs.getInt("event_id"));
                    ResultSet allAtendencesSet = pstmt2.executeQuery();
                    String eventEndDate = allAtendencesSet.getString("event_date") + ":"
                            + allAtendencesSet.getString("end_hour");
                    if (localDate.isBefore(LocalDateTime.parse(eventEndDate, dateformatter))) {
                        System.out.println("You can't register in this event because there is another event that hasn't finished yet");
                        return false;
                    }
                } catch (SQLException e) {
                    System.out.println("SQL Error: " + e);
                    return false;
                }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return false;
        }

        try (PreparedStatement preparedStatement = conn.prepareStatement(getEventSQL)) {
            preparedStatement.setString(1, code);
            ResultSet rs = preparedStatement.executeQuery();
            if (!rs.next())
                return false;
            codeId = rs.getInt("regist_code_id");
            eventId = rs.getInt("event_id");
            eventDate = rs.getString("event_date") + ":" + rs.getString("end_hour");
            eventStartDate = rs.getString("event_date") + ":" + rs.getString("start_hour");

        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return false;
        }



        try (PreparedStatement pstmt = conn.prepareStatement(getCodeSQL)) {
            pstmt.setInt(1, codeId);
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next())
                return false;
            codeDate = rs.getString("expiration_date");
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return false;
        }


        System.out.println("event - code " + eventDate + " *** " + codeDate + " local " + localDate);


        if (localDate.isAfter(LocalDateTime.parse(codeDate, dateformatter))
                || localDate.isAfter(LocalDateTime.parse(eventDate, dateformatter))
        ){
            return false;
        }

        if(localDate.isBefore(LocalDateTime.parse(eventStartDate, dateformatter))){
            return false;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(insertAttendennceSQL)) {
            pstmt.setInt(1, eventId);
            pstmt.setInt(2, userId);

            int afected = pstmt.executeUpdate();
            if (afected != 1)
                return false;

        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return false;
        }

        alterDbVersion();
        return true;
    }

    public ArrayList<Event> listAttendances(int userId) {
        String getEventIdSql = "SELECT * FROM attendances WHERE user_id == ?";
        String getEventSql = "SELECT * FROM events WHERE event_id == ?";
        ArrayList<Event> eventsAttended = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(getEventIdSql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                try (PreparedStatement pstmt2 = conn.prepareStatement(getEventSql)) {
                    pstmt2.setInt(1, rs.getInt("event_id"));
                    ResultSet r2 = pstmt2.executeQuery();
                    Event event = new Event(
                            r2.getInt("event_id"),
                            r2.getString("name"),
                            r2.getString("place"),
                            r2.getString("event_date"),
                            r2.getString("start_hour"),
                            r2.getString("end_hour"),
                            r2.getInt("creator_id"),
                            r2.getInt("regist_code_id"),
                            r2.getString("code")
                    );
                    eventsAttended.add(event);
                } catch (SQLException e) {
                    System.out.println("SQL Error: " + e);
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return eventsAttended;
    }

    public String listEvents() {
        String sql = "SELECT * FROM events ";
        StringBuilder out = new StringBuilder();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                out.append("Id: ").append(rs.getInt("event_id")).append(" | Name: ").append(rs.getString("name")).append("\n");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }

        return out.toString();
    }

    public ArrayList<User> listEventUsers(int eventId) {
        String sql = "SELECT " +
                "u.user_id, " +
                "u.email, " +
                "u.user_type " +
                "FROM users u " +
                "JOIN attendances at ON u.user_id = at.user_id " +
                "WHERE at.event_id = ?";

        ArrayList<User> eventUsers = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                User user = new User(
                        rs.getInt("user_id"),
                        rs.getString("email"),
                        null,
                        rs.getString("user_type")
                );
                eventUsers.add(user);
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return null;
        }

        return eventUsers;
    }

    public boolean createDatabase(){
        String sqlCreateTableDBVersions =
                "CREATE TABLE IF NOT EXISTS DB_versions (version_number INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT)";

        String sqlCreateTableUsers =
                "CREATE TABLE IF NOT EXISTS users" +
                        " (user_id INTEGER PRIMARY KEY ," +
                        " email TEXT, password TEXT, user_type TEXT)";

        String sqlCreateTableEvents =
                "CREATE TABLE IF NOT EXISTS events" +
                        " (event_id INTEGER PRIMARY KEY," +
                        " name TEXT, place TEXT, event_date TEXT, start_hour TEXT, end_hour TEXT," +
                        " creator_id INTEGER references users, regist_code_id INTEGER references regist_codes , code TEXT references regist_codes (code))";

        String sqlCreateTableAttendances =
                "CREATE TABLE IF NOT EXISTS attendances" +
                        " (event_id INTEGER NOT NULL, user_id INTEGER NOT NULL," +
                        " FOREIGN KEY (event_id) REFERENCES events (event_id)," +
                        " FOREIGN KEY (user_id) REFERENCES users (user_id))";

        String sqlCreateTableRegistCodes =
                "CREATE TABLE IF NOT EXISTS regist_codes" +
                        " (  code_id         integer\n" +
                        "        constraint register_codes_pk\n" +
                        "            primary key autoincrement,\n" +
                        "    expiration_date text,\n" +
                        "    event_id        integer\n" +
                        "        constraint regist_codes_events_event_ID_fk\n" +
                        "            references events,\n" +
                        "    code            text)";


        try(Connection conn = DriverManager.getConnection(URL);
            Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute(sqlCreateTableDBVersions);
            stmt.execute(sqlCreateTableUsers);
            stmt.execute(sqlCreateTableEvents);
            stmt.execute(sqlCreateTableAttendances);
            stmt.execute(sqlCreateTableRegistCodes);

        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return false;
        }

        return true;
    }

    public ArrayList<Event> listUserEventsPartialData(int userId){
        ArrayList<Event> events = new ArrayList<>();
        String sql = "SELECT event_id, name, event_date, start_hour, end_hour, code " +
                "FROM events " +
                "WHERE creator_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Event event = new Event(
                            rs.getInt("event_id"),
                            rs.getString("name"),
                            rs.getString("event_date"),
                            rs.getString("start_hour"),
                            rs.getString("end_hour"),
                            rs.getString("code")
                    );
                    events.add(event);
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return null;
        }

        return events;
    }


    public boolean deleteAttendence(int eventId, int userId) {
        String sql = "DELETE FROM attendances WHERE user_id = ? AND event_id = ?";
        //int eventId = Integer.parseInt(eventID);
        int affected;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, eventId);
            affected = pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return false;
        }

        if (affected == 0)
            return false;

        alterDbVersion();
        return true;

    }

    public int getUserIdFromEmail(String email){
        int user_id = -1;
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    user_id = rs.getInt("user_id");
                }
            }

        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
        }

        return user_id;
    }

    public boolean addAttendence(int event_id, String user_email) {
        int affected;
        String sql = "INSERT INTO attendances(event_id, user_id) VALUES(?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int userId = getUserIdFromEmail(user_email);

            if (userId < 0) {
                return false; // No user was found on given email
            }

            pstmt.setInt(1, event_id);
            pstmt.setInt(2, userId);
            affected = pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return false;
        }

        if (affected == 0) {
            return false;
        }

        alterDbVersion();
        return true;
    }

    public boolean deleteEvent(Object requestArguments) {
        String sql = "DELETE FROM events WHERE event_id = ?";
        int eventId = (int) requestArguments;
        int affected;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            affected = pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return false;
        }

        if (affected == 0)
            return false;

        String sqlRemoveAttendances = "DELETE FROM attendances WHERE event_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlRemoveAttendances)) {
            pstmt.setInt(1, eventId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return false;
        }


        alterDbVersion();
        return true;
    }

    public Event getEvent(int eventId){
        Event event = null;
        String sql = "SELECT * " +
                "FROM events " +
                "WHERE event_id = ?";

        String sqlcode = "SELECT expiration_date FROM regist_codes " +
                "WHERE event_id = ? " +
                "ORDER BY expiration_date DESC " +
                "LIMIT 1";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    event = new Event(
                            rs.getInt("event_id"),
                            rs.getString("name"),
                            rs.getString("place"),
                            rs.getString("event_date"),
                            rs.getString("start_hour"),
                            rs.getString("end_hour"),
                            rs.getInt("creator_id"),
                            rs.getInt("regist_code_id"),
                            rs.getString("code"));
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return null;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sqlcode)) {
            pstmt.setInt(1, eventId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("expiration_date");
                    if (event != null) {
                        event.setExpirationCodeDate(code);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return null;
        }

        return event;
    }

    public boolean editEvent(Event event) {
        String updateEventsSql = "UPDATE events SET " +
                "name = ?, place = ?, event_date = ?, start_hour = ?, " +
                "end_hour = ?, creator_id = ?, regist_code_id = ?, code = ? " +
                "WHERE event_id = ?";
        String updateRegistCodesSql = "UPDATE regist_codes SET " +
                "expiration_date = ?, code = ? WHERE event_id = ?";

        int affectedEvents;
        int affectedRegistCodes;

        try (PreparedStatement pstmtEvents = conn.prepareStatement(updateEventsSql);
             PreparedStatement pstmtRegistCodes = conn.prepareStatement(updateRegistCodesSql)) {

            pstmtEvents.setString(1, event.getName());
            pstmtEvents.setString(2, event.getPlace());
            pstmtEvents.setString(3, event.getEventDate());
            pstmtEvents.setString(4, event.getStartHour());
            pstmtEvents.setString(5, event.getEndHour());
            pstmtEvents.setInt(6, event.getCreatorId());
            pstmtEvents.setInt(7, event.getRegistCodeId());
            pstmtEvents.setString(8, event.getCode());
            pstmtEvents.setInt(9, event.getEventId());

            pstmtRegistCodes.setString(1, event.getExpirationCodeDate());
            pstmtRegistCodes.setString(2, event.getCode());
            pstmtRegistCodes.setInt(3, event.getEventId());

            affectedEvents = pstmtEvents.executeUpdate();
            affectedRegistCodes = pstmtRegistCodes.executeUpdate();

        } catch (SQLException e) {
            System.out.println("SQL Error: " + e);
            return false;
        }

        // both updates were successful
        return affectedEvents > 0 && affectedRegistCodes > 0;
    }
}
