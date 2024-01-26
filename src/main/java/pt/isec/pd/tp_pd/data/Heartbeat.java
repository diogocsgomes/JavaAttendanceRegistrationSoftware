package pt.isec.pd.tp_pd.data;

import java.io.Serializable;

public class Heartbeat implements Serializable {
    private final int registryListeningPort;
    private final String rmiServiceName;
    private final long localDatabaseVersion;

    public Heartbeat(int registryListeningPort, String rmiServiceName, long localDatabaseVersion) {
        this.registryListeningPort = registryListeningPort;
        this.rmiServiceName = rmiServiceName;
        this.localDatabaseVersion = localDatabaseVersion;
    }

    public int getRegistryListeningPort() {
        return registryListeningPort;
    }

    public String getRmiServiceName() {
        return rmiServiceName;
    }

    public long getLocalDatabaseVersion() {
        return localDatabaseVersion;
    }

    @Override
    public String toString() {
        return "[Heartbeat] RegistryListeningPort: " + registryListeningPort + " RmiServiceName: " + rmiServiceName + " DatabaseVersion: " + localDatabaseVersion;
    }
}
