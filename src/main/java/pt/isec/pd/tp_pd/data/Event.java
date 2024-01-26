package pt.isec.pd.tp_pd.data;

import java.io.Serializable;

public class Event implements Serializable {
    int eventId;
    public final String name;
    public String place;
    public final String eventDate;
    public final String startHour;
    final String endHour;
    int creatorId;
    int registCodeId;
    final String code;
    String expirationCodeDate;

    public Event(String name, String place, String eventDate, String startHour, String endHour, int creatorId, String code, String expirationCodeDate) {
        this.name = name;
        this.place = place;
        this.eventDate = eventDate;
        this.startHour = startHour;
        this.endHour = endHour;
        this.creatorId = creatorId;
        this.code = code;
        this.expirationCodeDate = expirationCodeDate;
    }



    public Event(int eventId, String name, String place, String eventDate, String startHour, String endHour, int creatorId, int registCodeId, String code) {
        this.eventId = eventId;
        this.name = name;
        this.place = place;
        this.eventDate = eventDate;
        this.startHour = startHour;
        this.endHour = endHour;
        this.creatorId = creatorId;
        this.registCodeId = registCodeId;
        this.code = code;
    }

    public Event(int eventId, String name, String eventDate, String startHour, String endHour, String code) {
        this.eventId = eventId;
        this.name = name;
        this.eventDate = eventDate;
        this.startHour = startHour;
        this.endHour = endHour;
        this.code = code;
    }

    public int getEventId() {
        return eventId;
    }

    public String getName() {
        return name;
    }

    public String getPlace() {
        return place;
    }

    public String getEventDate() {
        return eventDate;
    }

    public String getStartHour() {
        return startHour;
    }

    public String getEndHour() {
        return endHour;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public int getRegistCodeId() {
        return registCodeId;
    }

    public String getCode() {
        return code;
    }

    public String getExpirationCodeDate() {
        return expirationCodeDate;
    }

    public void setExpirationCodeDate(String expirationCodeDate) {
        this.expirationCodeDate = expirationCodeDate;
    }

}

