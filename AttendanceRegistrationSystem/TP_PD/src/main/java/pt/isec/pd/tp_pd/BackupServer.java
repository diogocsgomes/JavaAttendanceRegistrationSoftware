package pt.isec.pd.tp_pd;

import pt.isec.pd.tp_pd.data.Heartbeat;
import pt.isec.pd.tp_pd.interfaces.DatabaseBackupInterface;
import pt.isec.pd.tp_pd.interfaces.UpdateDatabaseBackupInterface;

import java.io.*;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class BackupServer implements UpdateDatabaseBackupInterface, Serializable {
    private static final byte[] BUFFER = new byte[312]; // long + int + 150 * char = 312 bytes: buffer for Heartbeat object
    private static boolean running = true;
    private static final int TIMEOUT = 30 * 1000; // 30 sec
    private static MulticastSocket socket;
    private File databaseDirectory;
    private static final String DATABASE_NAME = "backupDatabase.sqlite";
    private static int databaseVersion = 0;
    private String rmiServiceName;
    private int rmiServicePort;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Syntax: backupDatabaseDirectory");
            return;
        }

        try {
            InetAddress address = InetAddress.getByName("230.44.44.44");
            int port = 4444;

            File databaseDirectory = new File(args[0]);

            if (!isDirectoryValid(databaseDirectory)) return;

            socket = new MulticastSocket(port);
            socket.setSoTimeout(TIMEOUT);
            socket.joinGroup(address);

            System.out.println("--- Backup Server started ---");
            System.out.println("Waiting to receive the first heartbeat...");
            Heartbeat heartbeat = receiveHeartBeat();
            System.out.println("First heartbeat received: " + heartbeat);
            //System.out.println("Local Database Version: " + databaseVersion);

            if (databaseVersion == 0) {
                databaseVersion = (int) heartbeat.getLocalDatabaseVersion();
            }

            if (databaseVersion != heartbeat.getLocalDatabaseVersion()) {
                return;
            }

            BackupServer server = new BackupServer();

            server.rmiServicePort = heartbeat.getRegistryListeningPort();
            server.rmiServiceName = heartbeat.getRmiServiceName();
            server.databaseDirectory = databaseDirectory;

            /* Getting the RMI ready to read the database file*/
            Registry registry = LocateRegistry.getRegistry("localhost", heartbeat.getRegistryListeningPort());
            DatabaseBackupInterface backupService = (DatabaseBackupInterface) registry.lookup(heartbeat.getRmiServiceName());

            String backupDatabasePath = new File(databaseDirectory + File.separator + DATABASE_NAME).getCanonicalPath();

            FileOutputStream fileOutputStream = new FileOutputStream(backupDatabasePath);

            downloadDatabase(backupService, fileOutputStream);
            System.out.println("First download of the database is done.");

            backupService.addBackupServer(server);
            System.out.println("Added this server to the backupservice list.");

            fileOutputStream.close();

            while (running) {
                heartbeat = null;
                try {
                    heartbeat = BackupServer.receiveHeartBeat();
                    System.out.println(heartbeat);
                    //System.out.println("Local Database Version: " + databaseVersion);
                } catch (SocketTimeoutException e) {
                    System.out.println("No heartbeat received in the last 30 seconds");
                    System.out.println("Exiting the program");
                    running = false;
                }

//            if(heartbeat != null){
//                if (heartbeat.getLocalDatabaseVersion() != server.backupDatabaseVersion) {
//                    System.out.println(server.hashCode());
//                    running = false;
//                }
//            }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e);
        }
    }

    public static Heartbeat receiveHeartBeat() throws SocketTimeoutException {
        Heartbeat heartbeat = null;
        try {
            socket.setSoTimeout(30 * 1000);
            DatagramPacket dpkt = new DatagramPacket(BUFFER, BUFFER.length);
            socket.receive(dpkt);

            ByteArrayInputStream bin = new ByteArrayInputStream(dpkt.getData());
            ObjectInputStream ois = new ObjectInputStream(bin);
            heartbeat = (Heartbeat) ois.readObject();

        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException e) {
            System.out.println("I/O Error: " + e);
        } catch (ClassNotFoundException e) {
            System.out.println("Class Not Found Error: " + e);
        }
        return heartbeat;
    }

    private static int downloadDatabase(DatabaseBackupInterface backupService, FileOutputStream fileOutputStream) throws IOException {
        byte[] b;
        int offset = 0;
        while ((b = backupService.getDatabaseChunk(offset)) != null) {
            fileOutputStream.write(b, 0, b.length);
            offset += b.length;
        }
        return databaseVersion++;
    }

    private static boolean isDirectoryValid(File directory) {
        if (!directory.exists()) {
            System.out.println("The directory " + directory + " doesn't exist!");
            return false;
        }

        if (!directory.isDirectory()) {
            System.out.println("The path " + directory + " doesn't refer to a directory!");
            return false;
        }

        if (!directory.canRead()) {
            System.out.println("No permissions to read on the directory " + directory + "!");
            return false;
        }

        if (!directory.canWrite()) {
            System.out.println("No permissions to write on the directory " + directory + "!");
            return false;
        }

        var filesInDirectory = directory.listFiles();

        if (filesInDirectory == null) {
            System.out.println("Given path does not direct to folder.");
            return false;
        } else if (filesInDirectory.length != 0) {
            System.out.println("Directory " + directory + " must be empty!");
            return false;
        }

        return true;
    }

    public static long getDatabaseVersion() {
        return databaseVersion;
    }

    @Override
    public void update() throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            DatabaseBackupInterface backupService = (DatabaseBackupInterface) registry.lookup(rmiServiceName);
            String backupDatabasePath = new File(databaseDirectory + File.separator + DATABASE_NAME).getCanonicalPath();
            FileOutputStream fileOutputStream = new FileOutputStream(backupDatabasePath);

            int newDatabaseVersion = downloadDatabase(backupService, fileOutputStream);
            databaseVersion = newDatabaseVersion; // Atualizando a variável databaseVersion

            fileOutputStream.close();
        } catch (IOException e) {
            System.err.println("Error while downloading the database.");
            throw new RemoteException("Problem with coping the database");
        } catch (NotBoundException e) {
            System.err.println("Error while bounding the object from rmi service.");
            throw new RemoteException("Problem with object bounding.");
        }
    }
}
