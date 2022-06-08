package se.laz.casual.standalone;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.util.Objects;
import java.util.function.Supplier;

import static javax.transaction.xa.XAResource.TMSUCCESS;

public class TransactionWrapper
{
    private final TransactionManager transactionManager;
    private final Object transactLock = new Object();

    private TransactionWrapper(TransactionManager transactionManager)
    {
        this.transactionManager = transactionManager;
    }

    public static TransactionWrapper of(TransactionManager transactionManager)
    {
        Objects.requireNonNull(transactionManager, "transactionManager can not be null");
        return new TransactionWrapper(transactionManager);
    }

    public <T> T execute(Supplier<T> supplier, XAResource xaResource)
    {
        synchronized (transactLock)
        {
            try
            {
                transactionManager.getTransaction().enlistResource(xaResource);
                T answer = supplier.get();
                transactionManager.getTransaction().delistResource(xaResource, TMSUCCESS);
                return answer;
            }
            catch (RollbackException | SystemException e)
            {
                throw new TransactionException(e);
            }
        }
    }

}
