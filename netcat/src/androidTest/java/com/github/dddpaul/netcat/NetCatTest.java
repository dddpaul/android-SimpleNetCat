package com.github.dddpaul.netcat;

import android.util.Log;
import junit.framework.TestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.dddpaul.netcat.NetCater.Op.*;
import static com.github.dddpaul.netcat.NetCater.Result;

public class NetCatTest extends TestCase implements NetCatListener
{
    private final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    final String HOST = "192.168.122.1";
    final String PORT = "9999";

    NetCater netCat;
    CountDownLatch signal;

    @Override
    public void setUp() throws Exception
    {
        netCat = new NetCat( this );
    }

    @Override
    public void netCatIsStarted()
    {
        signal = new CountDownLatch( 1 );
    }

    @Override
    public void netCatIsCompleted( Result result )
    {
        signal.countDown();
    }

    @Override
    public void netCatIsFailed( Result e )
    {
        Log.e( CLASS_NAME, e.getErrorMessage() );
        signal.countDown();
    }

    public void testConnect() throws InterruptedException
    {
        netCat.execute( CONNECT.toString(), HOST, PORT );
        signal.await( 10, TimeUnit.SECONDS );
    }
}
