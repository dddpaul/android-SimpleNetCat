package com.github.dddpaul.netcat;

import android.util.Log;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAsyncTask;
import org.robolectric.shadows.ShadowLog;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.dddpaul.netcat.NetCater.Op.*;
import static com.github.dddpaul.netcat.NetCater.Result;

@Config( emulateSdk = 18 )
@RunWith( RobolectricTestRunner.class )
public class NetCatTest extends Assert implements NetCatListener
{
    private final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    final String HOST = "192.168.0.100";
    final String PORT = "9999";

    ShadowAsyncTask<String, Void, Result> shadowTask;
    NetCat netCat;
    Result result;
    CountDownLatch signal;

    @Before
    public void setUp() throws Exception
    {
        ShadowLog.stream = System.out;
        netCat = new NetCat( this );
        shadowTask = Robolectric.shadowOf( netCat.task );
    }

    @Override
    public void netCatIsStarted()
    {
        signal = new CountDownLatch( 1 );
    }

    @Override
    public void netCatIsCompleted( Result result )
    {
        this.result = result;
        signal.countDown();
    }

    @Override
    public void netCatIsFailed( Result result )
    {
        this.result = result;
        Log.e( CLASS_NAME, result.getErrorMessage() );
        signal.countDown();
    }

    @Test
    public void testConnect() throws InterruptedException
    {
        shadowTask.execute( CONNECT.toString(), HOST, PORT );
        signal.await( 5, TimeUnit.SECONDS );
        assertNotNull( result );
        assertEquals( CONNECT, result.op );
        if( result.exception == null ) {
            assertNotNull( result.getSocket() );
        }
    }
}
