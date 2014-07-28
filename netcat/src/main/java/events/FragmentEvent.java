package events;

import com.github.dddpaul.netcat.NetCater;

public class FragmentEvent
{
    public NetCater.Op op;
    public NetCater.State state;
    public String data;

    public FragmentEvent( NetCater.Op op, NetCater.State state, String data )
    {
        this.op = op;
        this.state = state;
        this.data = data;
    }

    public FragmentEvent( NetCater.Op op, String data )
    {
        this( op, null, data );
    }

    public FragmentEvent( NetCater.Op op )
    {
        this( op, null, null );
    }

    public FragmentEvent( NetCater.State state )
    {
        this( null, state, null );
    }
}
