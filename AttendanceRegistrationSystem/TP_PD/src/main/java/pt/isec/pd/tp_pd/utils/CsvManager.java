package pt.isec.pd.tp_pd.utils;

import pt.isec.pd.tp_pd.data.Event;
import pt.isec.pd.tp_pd.data.User;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;

public class CsvManager {

    private final File saveDirectory;

    public CsvManager() throws AccessDeniedException {
        boolean isSaveDirectoryCreated = false;
        saveDirectory = setPathToSaveDirectory();

        if (!saveDirectory.exists())
            isSaveDirectoryCreated = saveDirectory.mkdir();

        if (!isSaveDirectoryCreated)
            throw new AccessDeniedException("Save directory cannot be created in project directory.");
    }

    public CsvManager(File saveDirectory) {
        this.saveDirectory = saveDirectory;
    }

    private File setPathToSaveDirectory() {

        String workingDirectoryPath = System.getProperty("user.dir");
        System.out.println(workingDirectoryPath);
        return new File(
                workingDirectoryPath +
                        File.separator +
                        "Save");
    }

    public boolean WriteToCsvFile(User user, ArrayList<Event> events) {
        String fileName = "Events_" + user.getUser_id() + ".csv";
        try {
            String pathToSavedFile =
                    saveDirectory.getCanonicalPath() + File.separator + fileName;
            BufferedWriter writer = new BufferedWriter(new FileWriter(pathToSavedFile));

            writer.write("\"Número identificação\";\"Email\"\n");
            writer.write("\"" + user.getUser_id() + "\";\"" + user.getEmail() + "\"\n");
            writer.newLine();
            writer.write("\"Designação\";\"Local\";\"Data\";\"Hora início\"\n");
            for (Event e : events) {
                writer.write("\"" + e.name + "\";\"" + e.place + "\";\"" + e.eventDate + "\";\"" + e.startHour + "\"\n");
            }
            writer.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean WriteToCsvFile(Event event, Iterable<User> users) {
        String fileName = event.getName() + "AttendenceList.csv";
        try {
            String pathToSavedFile =
                    saveDirectory.getCanonicalPath() + File.separator + fileName;
            BufferedWriter writer = new BufferedWriter(new FileWriter(pathToSavedFile));
            writer.write("\"Designação\";\"" + event.name + "\"\n");
            writer.write("\"Local\";\"" + event.place + "\"\n");
            writer.write("\"Data\";\"" + event.eventDate.replace("-", "\";\"") + "\"\n");
            writer.write("\"Hora início\";\"" + event.startHour.replace(":", "\";\"") + "\"\n");
            writer.write("\"Hora fim\";\"" + event.startHour.replace(":", "\";\"") + "\"\n");
            writer.newLine();
            writer.write("\"Número identificação\";\"Email\"\n");
            for (User user: users) {
                writer.write( "\"" + user.getUser_id() + "\";\"" + user.getEmail() + "\"\n");
            }

            writer.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
