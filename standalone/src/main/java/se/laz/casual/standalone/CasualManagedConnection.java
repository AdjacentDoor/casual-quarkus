package se.laz.casual.standalone;

import java.util.Optional;

public interface CasualManagedConnection
{
    Optional<Caller> getCaller();
    void close();
}
