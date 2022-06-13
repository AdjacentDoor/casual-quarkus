package se.laz.casual.standalone;

public class ServiceCallFailedException extends RuntimeException
{
    private static final long serialVersionUID = 1L;
    public ServiceCallFailedException(String s)
    {
        super(s);
    }
}
