package pt.isec.pd.tp_pd;

import pt.isec.pd.tp_pd.data.*;
import pt.isec.pd.tp_pd.database.DatabaseManager;
import pt.isec.pd.tp_pd.interfaces.DatabaseBackupInterface;
import pt.isec.pd.tp_pd.interfaces.UpdateDatabaseBackupInterface;

import java.io.*;
import java.net.*;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

class SendHeartbeatThread extends Thread{
    static boolean running = true;

    public SendHeartbeatThread() {}


    // RUN METHOD IS EXECUTED WHEN THREAD FIRST STARTED
    public void run() {
        while (running) {
            try{
                MainServer.sendHeartBeat(); // Send heartbeat
                sleep(10 * 1000); // Wait 10 sec

            } catch (InterruptedException e){
                System.out.println("Error on sleep: " + e);
            }
        }
        System.out.println("Stopping SendHeartBeatThread");
    }
}

class ProcessClientThread extends Thread{
    static boolean running = true;
    private final Socket clientSocket;
    ClientRequest receivedRequest;
    final DatabaseManager databaseManager;
    ClientResponse response = null;

    public ProcessClientThread(Socket clientSocket, DatabaseManager databaseManager) {
        this.clientSocket = clientSocket;
        this.databaseManager = databaseManager;
    }

    // RUN METHOD IS EXECUTED WHEN THREAD FIRST STARTED
    public void run() {
        System.out.println("Created client processing thread for client " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
        try {
            ObjectInputStream oin = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream oout = new ObjectOutputStream(clientSocket.getOutputStream());

            // Receive Messages
            while (running) {
                receivedRequest = (ClientRequest) oin.readObject();

                System.out.println("[" + Thread.currentThread().getName() + "]" +
                        " Received " + receivedRequest.toString() + " [" + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + "]");

                // Verification for the received message
                switch (receivedRequest.getType()) {
                    case LOGIN -> {
                        User temp = databaseManager.login(receivedRequest.getUser().getEmail(), receivedRequest.getUser().getPassword());
                        response = new ClientResponse(temp);

                        if (response.user == null) {
                            response.type = ClientResponse.Type.INVALID_USER;
                        } else {
                            response.type = ClientResponse.Type.SUCCESS;
                        }
                    }
                    case REGISTER -> {
                        User temp = databaseManager.insertUser(
                                receivedRequest.getUser().getEmail(),
                                receivedRequest.getUser().getPassword(),
                                "participant",
                                receivedRequest.getUser().getUser_id());
                        response = new ClientResponse(temp);

                        if (response.user == null) {
                            response.type = ClientResponse.Type.ERROR;
                        } else {
                            response.type = ClientResponse.Type.SUCCESS;
                        }
                    }
                    case EDIT_PROFILE -> {
                        boolean isUserUpdated = databaseManager.editUser(
                                receivedRequest.getUser().getUser_id(),
                                receivedRequest.getUser().getEmail(),
                                receivedRequest.getUser().getPassword());
                        response = new ClientResponse(receivedRequest.getUser());

                        if (!isUserUpdated) {
                            response.type = ClientResponse.Type.ERROR;
                        } else {
                            response.type = ClientResponse.Type.SUCCESS;
                        }
                    }
                    case LOGOUT -> running = false;
                    case ADMIN_CREATE_EVENT -> {
                        Event event = receivedRequest.getEvent();
                        Event addedEvent = databaseManager
                                .insertEvent(event.getName(), event.getPlace(),
                                        event.getEventDate(), event.getStartHour(),
                                        event.getEndHour(), event.getCreatorId(),
                                        event.getCode(), event.getExpirationCodeDate());

                        response = new ClientResponse(receivedRequest.getUser());

                        if (addedEvent != null) {
                            response.type = ClientResponse.Type.SUCCESS;
                        } else
                            response.type = ClientResponse.Type.ERROR;
                    }
                    case ADMIN_GENERATE_ATTENDANCE_CODE -> {
                        String code;
                        boolean doesCodeExist = true;
                        code = generateRandomString(6);

                        for (int i = 0; i < 5; ++i) { // make for loop not to create deadlock
                            doesCodeExist = databaseManager.doesCodeExist(code);
                            if (!doesCodeExist) break;
                        }
                        if (doesCodeExist) code = null;

                        response = new ClientResponse(receivedRequest.getUser());
                        if (code != null) {
                            response.setCode(code);
                            response.type = ClientResponse.Type.SUCCESS;
                        } else
                            response.type = ClientResponse.Type.ERROR;
                    }
                    case ADMIN_CONSULT_EVENT_ATTENDANCES -> {
                        ArrayList<User> users;
                        int eventId = (Integer) receivedRequest.getRequestArguments();
                        users = databaseManager.listEventUsers(eventId);
                        response = new ClientResponse(receivedRequest.getUser());

                        if (users != null) {
                            response.setReponseResult(users);
                            response.type = ClientResponse.Type.SUCCESS;
                        } else
                            response.type = ClientResponse.Type.ERROR;
                    }
                    case ADMIN_CONSULT_EVENTS -> {
                        ArrayList<Event> events;
                        int userId = receivedRequest.getUser().getUser_id();
                        events = databaseManager.listUserEventsPartialData(userId);
                        response = new ClientResponse(receivedRequest.getUser());

                        if (events != null) {
                            response.setReponseResult(events);
                            response.type = ClientResponse.Type.SUCCESS;
                        } else
                            response.type = ClientResponse.Type.ERROR;
                    }
                    case ADMIN_GET_EVENT -> {
                        int eventId = (int) receivedRequest.getRequestArguments();
                        Event event = databaseManager.getEvent(eventId);
                        response = new ClientResponse(receivedRequest.getUser());

                        if (event != null) {
                            response.setReponseResult(event);
                            response.type = ClientResponse.Type.SUCCESS;
                        } else
                            response.type = ClientResponse.Type.ERROR;
                    }
                    case ADMIN_EDIT_EVENT -> {

                        Event event = receivedRequest.getEvent();
                        boolean hasAttendances;
                        ArrayList<User> users = databaseManager.listEventUsers(event.getEventId());
                        if(users == null || users.size() == 0) {

                            hasAttendances = false;
                        }
                        else
                        {
                            hasAttendances = true;
                        }
                        if(!hasAttendances) {
                            boolean isEventUpdated = databaseManager.editEvent(event);


                            if (isEventUpdated) {
                                response.type = ClientResponse.Type.SUCCESS;
                            } else {
                                response.type = ClientResponse.Type.ERROR;
                            }
                        }
                        else  {
                            response.type = ClientResponse.Type.ERROR;


                        }


                    }
                    default -> response = new ClientResponse(null, ClientResponse.Type.ERROR);

                    case PARTICIPANT_CONSULT_ATTENDANCES -> {
                        ArrayList<Event> attendances = databaseManager.listAttendances(receivedRequest.getUser().getUser_id());
                        response = new ClientResponse(receivedRequest.getUser());
                        if (attendances != null) {
                            response.type = ClientResponse.Type.SUCCESS;
                            response.setAttendances(attendances);
                        } else
                            response.type = ClientResponse.Type.ERROR;
                    }
                    case PARTICIPANT_REGISTER_ATTENDANCE -> {
                        boolean isRegistered = databaseManager.registerAttendence(receivedRequest.getCode(), receivedRequest.getUser().getUser_id());

                        response = new ClientResponse(receivedRequest.getUser());

                        if (isRegistered) {
                            response.type = ClientResponse.Type.SUCCESS;
                        } else
                            response.type = ClientResponse.Type.ERROR;
                    }
                    case ADMIN_DELETE_PARTICIPANT_ATTENDANCE -> {
                        boolean isDeleted = databaseManager.deleteAttendence(receivedRequest.getEventEditID(), receivedRequest.getUserEditID());

                        response = new ClientResponse(receivedRequest.getUser());

                        if (isDeleted) {
                            response.type = ClientResponse.Type.SUCCESS;
                        } else
                            response.type = ClientResponse.Type.ERROR;
                    }
                    case ADMIN_ADD_PARTICIPANT_ATTENDANCE -> {
                        boolean isAdded = databaseManager.addAttendence(receivedRequest.getEventEditID(), receivedRequest.getUserEditEmail());

                        response = new ClientResponse(receivedRequest.getUser());

                        if (isAdded) {
                            response.type = ClientResponse.Type.SUCCESS;
                        } else
                            response.type = ClientResponse.Type.ERROR;
                    }
                    case ADMIN_DELETE_EVENT -> {
                        boolean hasAttendances;
                        ArrayList<User> users = databaseManager.listEventUsers((int) receivedRequest.getRequestArguments());
                        if(users == null || users.size() == 0)
                            hasAttendances = false;
                        else
                            hasAttendances = true;
                        if(!hasAttendances) {
                            boolean isDeleted = databaseManager.deleteEvent(receivedRequest.getRequestArguments());

                            response = new ClientResponse(receivedRequest.getUser());

                            if (isDeleted) {
                                response.type = ClientResponse.Type.SUCCESS;
                            } else
                                response.type = ClientResponse.Type.ERROR;
                        }
                        else {
                            response.type = ClientResponse.Type.ERROR;
                        }
                    }

                    case ADMIN_GET_EVENTS_BY_USER -> {
                        ArrayList<Event> events;
                        int userId = (int) receivedRequest.getRequestArguments();
                        events = databaseManager.listAttendances(userId);
                        response = new ClientResponse(receivedRequest.getUser());

                        if (events != null) {
                            response.setReponseResult(events);

                            response.type = ClientResponse.Type.SUCCESS;
                        } else
                            response.type = ClientResponse.Type.ERROR;
                    }
                }

                try {
                    oout.writeObject(response);
                    oout.flush();
                } catch (IOException e) {
                    System.out.println("I/O Error: " + e);
                }
            }

            clientSocket.close();

        } catch (ClassNotFoundException e) {
            System.out.println("Class Not Found Error: " + e);
        } catch (IOException e) {
            if (this.receivedRequest != null && receivedRequest.getUser() != null) {
                System.out.println("Client with email " + receivedRequest.getUser().getEmail() + " disconnected");
            } else {
                System.out.println("Anonymous client disconnected");
            }
        }

        System.out.println("Stopping ProcessClientThread");
    }

    public String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder stringBuilder = new StringBuilder();

        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            stringBuilder.append(randomChar);
        }

        return stringBuilder.toString();
    }
}

public class MainServer extends UnicastRemoteObject implements Remote, DatabaseBackupInterface, Serializable{
    static long localDatabaseVersion = 0L;
    static int tcpServerPort;
    static int registryPort;
    static String rmiServiceName;
    static DatabaseManager databaseManager;
    //static File databaseDirectory;
    private static File databaseDirectory;
    private final int MAX_CHUNK_SIZE = 10000; // bytes
    private final String DATABASE_NAME = "DP_ProjectDB.sqlite";

    static boolean running = true;
    static ArrayList<Thread> threads = new ArrayList<>();
    static Thread t = null;


    public MainServer(File databaseDirectory) throws RemoteException {
        MainServer.databaseDirectory = databaseDirectory;
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Syntax: tcpServerPort registryPort rmiServiceName databaseDirectory");
            return;
        }

        tcpServerPort = Integer.parseInt(args[0]);
        registryPort = Integer.parseInt(args[1]);
        rmiServiceName = args[2];

        // Get the directory where the MainServer class is located

        databaseDirectory = new File(args[3].trim());

        if (!databaseDirectory.exists()) {
            System.out.println("The directory " + databaseDirectory + " doesn't exist!");
            return;
        }

        if (!databaseDirectory.isDirectory()) {
            System.out.println("The path " + databaseDirectory + " doesn't refer to a directory!");
            return;
        }

        if (!databaseDirectory.canRead()) {
            System.out.println("No permissions to read on the directory " + databaseDirectory + "!");
            return;
        }

        try {
            // Creates the service
            MainServer mainServer = new MainServer(databaseDirectory);

            // Launches the rmi registry in the given port
            try {
                mainServer.setRegistration(registryPort);
            } catch (RemoteException e) {
                System.out.println("A server is already in execution on the local machine\nExiting...");
                System.exit(1);
            }

            databaseManager = new DatabaseManager(mainServer);
            localDatabaseVersion = databaseManager.getDatabaseVersion();
            System.out.println("DatabaseBackupService created and in execution...");

            String registry = "localhost";
            String registration = "rmi://" + registry + "/" + rmiServiceName;

            // Registers the service in the local rmi registry so that the clients can locate it
            Naming.bind(registration, mainServer);

            System.out.println("Service " + rmiServiceName + " registered in registry with port " + registryPort);

            t = new SendHeartbeatThread();
            t.start();
            threads.add(t);

            // To terminate an RMI service of type UnicastRemoteObject:
            // UnicastRemoteObject.unexportObject(backupService, true);

        } catch (RemoteException e) {
            System.out.println("Remote error - " + e);
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error - " + e);
            System.exit(1);
        }

        try (ServerSocket serverSocket = new ServerSocket(tcpServerPort)) {
            System.out.println("Server is running fine at address " + serverSocket.getInetAddress().getHostName() + " and port " + serverSocket.getLocalPort());
            while (running) {
                Socket clientSocket = serverSocket.accept();
                t = new ProcessClientThread(clientSocket, databaseManager);
                t.start();
                threads.add(t);
            }

        } catch (NumberFormatException e) {
            System.out.println("The TCP listening port should be a positive integer.");
        } catch (SocketException e) {
            System.out.println("Error on TCP serverSocket:\n\t" + e);
        } catch (IOException e) {
            System.out.println("Error on access to the serverSocket:\n\t" + e);
        }

        System.out.println("Waiting to join the threads");

        for (Thread t:threads) {
            try {
                t.join();
            } catch (InterruptedException e){
                System.out.println("Error joining threads");
            }
        }
    }

    public static void sendHeartBeat() {
        try{
            // Create the heartbeat
            Heartbeat heartbeat = new Heartbeat(registryPort, rmiServiceName, localDatabaseVersion);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);

            oos.writeObject(heartbeat);

            try(MulticastSocket socket = new MulticastSocket()) {
                byte[] data = bos.toByteArray();

                // Define the multicast address and port
                InetAddress group = InetAddress.getByName("230.44.44.44");
                int port = 4444;

                // Create a DatagramPacket with the pt.isec.pd.tp_pd.data, address, and port
                DatagramPacket dpkt = new DatagramPacket(data, data.length, group, port);

                socket.send(dpkt);
                System.out.println("[Heartbeat sent with version " + heartbeat.getLocalDatabaseVersion() + "]");
            }
        } catch (UnknownHostException e) {
            System.out.println("Unknown Host Error: " + e);
        } catch (IOException e) {
            System.out.println("I/O Error: " + e);
        }
    }

    public static void updateDatabase(int version) {
        localDatabaseVersion = version;
        try{
            DatabaseBackupInterface.updateBackupServers();
        } catch (RemoteException e) {
            System.out.println("Remote Error: " + e);
        }
    }

    @Override
    public byte[] getDatabaseChunk(long offset) throws IOException {
        String requestedCanonicalFilePath;
        byte[] fileChunk = new byte[MAX_CHUNK_SIZE];
        int nbytes;

        try {
            // Verify if the requested file exists and is in the directory
            requestedCanonicalFilePath = new File(databaseDirectory + File.separator + DATABASE_NAME).getCanonicalPath();

            if (!requestedCanonicalFilePath.startsWith(databaseDirectory.getCanonicalPath() + File.separator)) {
                System.out.println("The access to " + requestedCanonicalFilePath + " is not permitted!");
                System.out.println("The base directory doesn't correspond to " + databaseDirectory.getCanonicalPath() + "!");
                throw new FileNotFoundException(databaseDirectory.getCanonicalPath());
            }

            // Open the requested file to read
            try (FileInputStream requestedFileInputStream = new FileInputStream(requestedCanonicalFilePath)) {

                // Get a byte block of the file and put it into the fileChunk array, omitting the first offset bytes
                requestedFileInputStream.skip(offset);
                nbytes = requestedFileInputStream.read(fileChunk);

                if (nbytes == -1) { //EOF
                    return null;
                }

                // If fileChunk isn't completely filled (MAX_CHUNK_SIZE), use an auxiliary array with the correct size for the read bytes
                if (nbytes < fileChunk.length) {
                    return Arrays.copyOf(fileChunk, nbytes);
                }

                return fileChunk;
            }

        } catch (FileNotFoundException e) {   // Subclass of IOException
            System.out.println("The exception {" + e + "} occurred while trying to open the file!");
            throw new FileNotFoundException(databaseDirectory.toString());
        } catch (IOException e) {
            System.out.println("I/O exception: \n\t" + e);
            throw new IOException(databaseDirectory.toString(), e.getCause());
        }
    }

    @Override
    public void addBackupServer(UpdateDatabaseBackupInterface observer) throws RemoteException {
        synchronized (OBSERVERS) {
            if (!OBSERVERS.contains(observer)) {
                OBSERVERS.add(observer);
                System.out.println("Backup server added.");
            }

        }
    }

    public void setRegistration(int port) throws RemoteException {
        LocateRegistry.createRegistry(port);
    }
}
