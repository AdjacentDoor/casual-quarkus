package se.laz.casual.standalone;

public class QueueOperationFailedException extends RuntimeException
{
    private static final long serialVersionUID = 1L;
    public QueueOperationFailedException(String s)
    {
        super(s);
    }
}
