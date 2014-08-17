/**
 * Require nc binary (netcat-openbsd package for Debian/Ubuntu).
 */
package com.github.dddpaul.netcat;

import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.dddpaul.netcat.NetCater.Op.CONNECT;
import static com.github.dddpaul.netcat.NetCater.Op.LISTEN;
import static com.github.dddpaul.netcat.NetCater.Proto;
import static com.github.dddpaul.netcat.NetCater.Result;
import static org.hamcrest.core.Is.is;

@Config( emulateSdk = 18 )
@RunWith( RobolectricTestRunner.class )
public class UdpNetCatTest extends NetCatTest implements NetCatListener
{
    @Before
    public void setUp() throws Exception
    {
        netCat = new UdpNetCat( this );
        inputFromTest = INPUT_TEST + "\n";
        inputFromProcess = INPUT_NC +"\n";
    }

    @After
    public void tearDown() throws InterruptedException
    {
        disconnect();
        process.destroy();
    }

    @Test
    public void testUdpConnect() throws IOException, InterruptedException
    {
        int port = 9998;
        List<String> listener = prepareNetCatProcess( Proto.UDP, true, port );
        process = new ProcessBuilder( listener ).redirectErrorStream( true ).start();

        // Execute connect operation after some delay
        Thread.sleep( 500 );
        connect( port );

        send();
        receive();
    }

    @Test
    public void testUdpListen() throws IOException, InterruptedException
    {
        int port = 9997;

        // Connect to NetCat by external nc after some delay required for NetCat listener to start
        final List<String> dialer = prepareNetCatProcess( Proto.UDP, false, port );
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    Thread.sleep( 500 );
                    process = new ProcessBuilder( dialer ).redirectErrorStream( true ).start();
                } catch( Exception e ) {
                    e.printStackTrace();
                }
            }
        } ).start();

        // Start NetCat listener
        listen( port );

        receive();
        send();
    }

    @Override
    public void netCatIsStarted()
    {
        latch = new CountDownLatch( 1 );
    }

    @Override
    public void netCatIsCompleted( Result result )
    {
        this.result = result;
        latch.countDown();
    }

    @Override
    public void netCatIsFailed( Result result )
    {
        this.result = result;
        Log.e( CLASS_NAME, result.getErrorMessage() );
        latch.countDown();
    }

    public void connect( int port ) throws InterruptedException
    {
        netCat.execute( CONNECT.toString(), Proto.UDP.toString(), HOST, String.valueOf( port ) );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertNull( result.exception );
        assertThat( result.op, is( CONNECT ));
        assertNotNull( result.getSocket() );
    }

    public void listen( int port ) throws InterruptedException
    {
        netCat.execute( LISTEN.toString(), Proto.UDP.toString(), String.valueOf( port ) );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertNull( result.exception );
        assertThat( result.op, is( LISTEN ));
        assertNotNull( result.getSocket() );
    }
}
