package se.laz.casual.standalone.outbound;

import se.laz.casual.network.ProtocolVersion;
import se.laz.casual.network.outbound.NetworkListener;

import javax.transaction.TransactionManager;
import java.net.InetSocketAddress;
import java.util.UUID;

public class CallerProducer
{
    private static final String DOMAIN_NAME = "JAVA-QUARKUS_TEST_APP-" + UUID.randomUUID();
    private static final int RESOURCE_MANAGER_ID = 42;


    public Caller createCaller(TransactionManager transactionManager, String host, int port, NetworkListener networkListener)
    {
        InetSocketAddress address = new InetSocketAddress(host, port);
        return CallerImpl.createBuilder()
                         .withAddress(address)
                         .withDomainId(UUID.randomUUID())
                         .withDomainName(DOMAIN_NAME)
                         .withNetworkListener(networkListener)
                         .withProtocolVersion(ProtocolVersion.VERSION_1_0)
                         .withResourceManagerId(RESOURCE_MANAGER_ID)
                         .withTransactionManager(transactionManager)
                         .build();
    }
}
