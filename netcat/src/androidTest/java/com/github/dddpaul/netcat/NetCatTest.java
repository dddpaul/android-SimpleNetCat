package com.github.dddpaul.netcat;

import android.util.Log;
import junit.framework.TestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NetCatTest extends TestCase implements NetCatListener
{
    private final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    final String HOST = "192.168.122.1";
    final String PORT = "9999";

    NetCat netCat;
    CountDownLatch signal;

    @Override
    public void setUp() throws Exception
    {
        netCat = new NetCat( System.out );
        netCat.setListener( this );
    }

    @Override
    public void netCatIsStarted()
    {
        signal = new CountDownLatch( 1 );
    }

    @Override
    public void netCatIsCompleted()
    {
        signal.countDown();
    }

    @Override
    public void netCatIsFailed( Exception e )
    {
        Log.e( CLASS_NAME, e.getMessage() );
        signal.countDown();
    }

    public void test() throws InterruptedException
    {
        netCat.execute( HOST, PORT );
        signal.await( 10, TimeUnit.SECONDS );
    }
}
