package events;

import com.github.dddpaul.netcat.NetCater;

public class ActivityEvent
{
    public NetCater.State netCatState;

    public ActivityEvent( NetCater.State state )
    {
        this.netCatState = state;
    }
}
