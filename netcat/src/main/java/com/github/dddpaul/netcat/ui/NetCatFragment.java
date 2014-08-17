package com.github.dddpaul.netcat.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.github.dddpaul.netcat.NetCatListener;
import com.github.dddpaul.netcat.NetCater;
import com.github.dddpaul.netcat.TcpNetCat;
import com.github.dddpaul.netcat.UdpNetCat;

import static com.github.dddpaul.netcat.NetCater.*;

public class NetCatFragment extends Fragment
{
    private final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    private NetCater netCat;

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
    }

    public NetCater getNetCat()
    {
        return netCat;
    }

    public NetCater getOrCreateNetCat( Proto proto, NetCatListener listener )
    {
        switch( proto ) {
            case TCP:
                if( netCat != null && netCat instanceof TcpNetCat ) {
                    return netCat;
                }
                return new TcpNetCat( listener );
            case UDP:
                if( netCat != null && netCat instanceof UdpNetCat ) {
                    return netCat;
                }
                return new UdpNetCat( listener );
        }
        return null;
    }
}
