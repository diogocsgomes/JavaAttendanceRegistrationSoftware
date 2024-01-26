package pt.isec.pd.tp_pd.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface UpdateDatabaseBackupInterface extends Remote {
    void update() throws RemoteException;
}
