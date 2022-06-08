package se.laz.casual.standalone;

import se.laz.casual.config.ConfigurationException;
import se.laz.casual.network.ProtocolVersion;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class CallerPool
{
    public static String CASUAL_HOST_ENV = "CASUAL_HOST";
    public static String CASUAL_PORT_ENV = "CASUAL_PORT";
    public static String CASUAL_POOL_SIZE = "CASUAL_POOL_SIZE";
    public static String DOMAIN_NAME = "JAVA-TEST_APP-" + UUID.randomUUID();
    private final static List<Caller> pool = createPools();
    private final static Object poolLock = new Object();

    private CallerPool()
    {}

    public static CallerPool of()
    {
        return new CallerPool();
    }

    public Caller getCaller()
    {
        return RandomEntry.getRandomEntry(pool);
    }

    private static List<Caller> createPools()
    {
        final int poolSize = Integer.parseInt(Optional.ofNullable(System.getenv(CASUAL_POOL_SIZE)).orElseThrow(() -> new ConfigurationException(CASUAL_POOL_SIZE + " not set")));
        List<Caller> p = new ArrayList<>();
        for(int i = 0; i < poolSize; ++i)
        {
            p.add(createCaller());
        }
        return p;
    }

    private static Caller createCaller()
    {
        final String hostName = Optional.ofNullable(System.getenv(CASUAL_HOST_ENV)).orElseThrow(() -> new ConfigurationException(CASUAL_HOST_ENV + " not set"));
        final int port = Integer.parseInt(Optional.ofNullable(System.getenv(CASUAL_PORT_ENV)).orElseThrow(() -> new ConfigurationException(CASUAL_PORT_ENV + " not set")));
        InetSocketAddress address = new InetSocketAddress(hostName, port);
        return CallerImpl.createBuilder()
                         .withAddress(address)
                         .withDomainId(UUID.randomUUID())
                         .withDomainName(DOMAIN_NAME)
                         .withNetworkListener(CallerPool::handleDisconnected)
                         .withProtocolVersion(ProtocolVersion.VERSION_1_0)
                         .withResourceManagerId(42)
                         .build();
    }

    private static void handleDisconnected()
    {
        synchronized (poolLock)
        {
            long numberOfDisconnected = pool.stream()
                                            .filter(item -> item.isDisconnected())
                                            .collect(Collectors.counting());
            pool.removeIf(item -> item.isDisconnected());
            for(int i = 0; i < numberOfDisconnected; ++numberOfDisconnected)
            {
                pool.add(createCaller());
            }
        }
    }
}
