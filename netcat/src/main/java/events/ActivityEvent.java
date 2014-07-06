package events;

public class ActivityEvent
{
    public Integer position;
    public Boolean isDisconnectButtonVisible;

    public ActivityEvent( Integer position, Boolean isDisconnectButtonVisible )
    {
        this.position = position;
        this.isDisconnectButtonVisible = isDisconnectButtonVisible;
    }

    public ActivityEvent( int position )
    {
        this( position, null );
    }

    public ActivityEvent( Boolean isDisconnectButtonVisible )
    {
        this( null, isDisconnectButtonVisible );
    }
}
