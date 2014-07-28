package com.github.dddpaul.netcat.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.github.dddpaul.netcat.NetCatModule;
import com.github.dddpaul.netcat.NetCater;

import javax.inject.Inject;

import dagger.ObjectGraph;
import de.greenrobot.event.EventBus;
import events.FragmentEvent;

import static com.github.dddpaul.netcat.NetCater.State.IDLE;

public class NetCatFragment extends Fragment
{
    private final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    @Inject
    protected NetCater netCat;

    public NetCatFragment() {}

    public static NetCatFragment newInstance()
    {
        return new NetCatFragment();
    }

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setRetainInstance( true );
        ObjectGraph.create( new NetCatModule() ).inject( this );
        EventBus.getDefault().post( new FragmentEvent( IDLE ));
    }

    public NetCater getNetCat()
    {
        return netCat;
    }
}
