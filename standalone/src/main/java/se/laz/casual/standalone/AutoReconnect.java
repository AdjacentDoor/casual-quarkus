package se.laz.casual.standalone;

import se.laz.casual.network.outbound.NetworkListener;
import se.laz.casual.standalone.Caller;

import javax.transaction.TransactionManager;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class AutoReconnect implements Runnable
{
    private static final Logger LOG = Logger.getLogger(AutoReconnect.class.getName());
    private final ReconnectAble reconnectAble;
    private final String host;
    private final StaggeredOptions staggeredOptions;
    private final ScheduledExecutorService scheduledExecutorService;
    private final NetworkListener networkListener;
    private int port;
    private TransactionManager transactionManager;

    private AutoReconnect(ReconnectAble reconnectAble, String host, int port, TransactionManager transactionManager, StaggeredOptions staggeredOptions, NetworkListener networkListener)
    {
        this.reconnectAble = reconnectAble;
        this.host = host;
        this.port = port;
        this.transactionManager = transactionManager;
        this.staggeredOptions = staggeredOptions;
        this.networkListener = networkListener;
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.schedule(this, staggeredOptions.getNext().toMillis(), TimeUnit.MILLISECONDS);
    }

    public static AutoReconnect of(ReconnectAble reconnectAble, String host, int port, TransactionManager transactionManager, StaggeredOptions staggeredOptions, NetworkListener networkListener)
    {
        Objects.requireNonNull(reconnectAble, "reconnectAble can not be null");
        Objects.requireNonNull(host, "host can not be null");
        Objects.requireNonNull(transactionManager, "transactionManager can not be null");
        Objects.requireNonNull(staggeredOptions, "staggeredOptions can not be null");
        return new AutoReconnect(reconnectAble, host, port, transactionManager, staggeredOptions, networkListener);
    }

    @Override
    public void run()
    {
        try
        {
            Caller caller = new CallerProducer().createCaller(transactionManager, this.host, this.port, networkListener);
            reconnectAble.setCaller(caller);
        }
        catch(Exception e)
        {
            scheduledExecutorService.schedule(this, staggeredOptions.getNext().toMillis(), TimeUnit.MILLISECONDS);
        }
    }
}