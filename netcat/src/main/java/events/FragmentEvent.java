package events;

import com.github.dddpaul.netcat.NetCater;

public class FragmentEvent
{
    public NetCater.Op op;
    public String data;

    public FragmentEvent( NetCater.Op op, String data )
    {
        this.op = op;
        this.data = data;
    }

    public FragmentEvent( NetCater.Op op )
    {
        this( op, null );
    }
}
