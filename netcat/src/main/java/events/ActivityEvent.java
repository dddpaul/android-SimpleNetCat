package events;

import com.github.dddpaul.netcat.NetCater;

public class ActivityEvent
{
    public NetCater.State netCatState;
    public String text;

    public ActivityEvent( NetCater.State netCatState, String text )
    {
        this.netCatState = netCatState;
        this.text = text;
    }

    public ActivityEvent( NetCater.State state )
    {
        this( state, null );
    }
}
