package pt.isec.pd.tp_pd.interfaces;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface DatabaseBackupInterface extends Remote {
    final ArrayList<UpdateDatabaseBackupInterface> OBSERVERS = new ArrayList<>();
    byte[] getDatabaseChunk(long offset) throws IOException;
    void addBackupServer(UpdateDatabaseBackupInterface observer) throws RemoteException; // adds observer, register to main server

    static void removeBackupServer(UpdateDatabaseBackupInterface observer) throws RemoteException // removes observer
    {
        synchronized (OBSERVERS) {
            if (OBSERVERS.contains(observer)) {
                OBSERVERS.remove(observer);
                System.out.println("Backup server removed.");
            }
        }
    }

    static void updateBackupServers() throws RemoteException { // notify observer
        synchronized (OBSERVERS) {
            for (UpdateDatabaseBackupInterface backup : OBSERVERS) {
                try {
                    backup.update();
                } catch (RemoteException e) {
                    removeBackupServer(backup);
                    System.out.println("Observer not accessible.");
                }
            }
        }
    }
}
