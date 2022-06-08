package se.laz.casual.quarkus;

import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import se.laz.casual.api.buffer.CasualBuffer;
import se.laz.casual.api.buffer.ServiceReturn;
import se.laz.casual.api.buffer.type.OctetBuffer;
import se.laz.casual.api.flags.AtmiFlags;
import se.laz.casual.api.flags.Flag;
import se.laz.casual.api.flags.ServiceReturnState;
import se.laz.casual.standalone.Caller;
import se.laz.casual.standalone.CasualManagedConnection;
import se.laz.casual.standalone.CasualManagedConnectionImpl;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

@Path("/casual")
public class CasualResource
{
    private static final Logger LOG = Logger.getLogger(CasualResource.class.getName());
    private CasualManagedConnection managedConnection;

    @Inject
    public CasualResource(
             TransactionManager transactionManager,
             @ConfigProperty(name = "casual.host") String host,
             @ConfigProperty(name = "casual.port") String port)
    {
        managedConnection = CasualManagedConnectionImpl.of(transactionManager, host, Integer.parseInt(port));
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String ping()
    {
        return "Hello world";
    }

    @Transactional
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Path("{serviceName}")
    public Response serviceRequest(@PathParam("serviceName") String serviceName,
                                   InputStream inputStream,
                                   @DefaultValue("true") @QueryParam("trans") boolean trans)
    {
        try
        {
            byte[] data = IOUtils.toByteArray(inputStream);
            Flag<AtmiFlags> flags = trans ? Flag.of(AtmiFlags.NOFLAG) : Flag.of(AtmiFlags.TPNOTRAN);
            OctetBuffer buffer = OctetBuffer.of(data);
            return Response.ok().entity(makeCasualCall(buffer, serviceName, flags).getBytes().get(0)).build();
        }
        catch (Exception e)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return Response.serverError().entity(sw.toString()).build();
        }
    }

    @PreDestroy
    public void goingAway()
    {
        LOG.warning(() -> "Bean going away, closing caller");
        managedConnection.close();
    }

    private CasualBuffer makeCasualCall(CasualBuffer msg, String serviceName, Flag<AtmiFlags> flags)
    {
        Caller caller = managedConnection.getCaller().orElseThrow(() -> new RuntimeException("currently no caller, either never connected or disconnected and not yet reconnected"));
        ServiceReturn<CasualBuffer> reply = caller.tpcall(serviceName, msg, flags);
        if(reply.getServiceReturnState() == ServiceReturnState.TPSUCCESS)
        {
            return reply.getReplyBuffer();
        }
        throw new ServiceCallFailedException("tpcall failed: " + reply.getErrorState());
    }
}