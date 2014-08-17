/**
 * Require nc binary (netcat-openbsd package for Debian/Ubuntu).
 */
package com.github.dddpaul.netcat;

import android.util.Log;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.dddpaul.netcat.NetCater.Op.CONNECT;
import static com.github.dddpaul.netcat.NetCater.Op.DISCONNECT;
import static com.github.dddpaul.netcat.NetCater.Op.LISTEN;
import static com.github.dddpaul.netcat.NetCater.Op.RECEIVE;
import static com.github.dddpaul.netcat.NetCater.Op.SEND;
import static com.github.dddpaul.netcat.NetCater.Proto;
import static com.github.dddpaul.netcat.NetCater.Result;
import static org.hamcrest.core.Is.is;

@Config( emulateSdk = 18 )
@RunWith( RobolectricTestRunner.class )
public class TcpNetCatTest extends Assert implements NetCatListener
{
    final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    NetCater netCat;
    Result result;
    CountDownLatch latch;
    Process process;
    String inputFromTest, inputFromProcess;

    @BeforeClass
    public static void init()
    {
        ShadowLog.stream = System.out;
    }

    @Before
    public void setUp() throws Exception
    {
        netCat = new TcpNetCat( this );
        inputFromTest = TestUtils.INPUT_TEST;
        inputFromProcess = TestUtils.INPUT_NC;
    }

    @After
    public void tearDown() throws InterruptedException
    {
        disconnect();
        process.destroy();
    }

    @Test
    public void testTcpConnect() throws IOException, InterruptedException
    {
        int port = 9998;
        List<String> listener = TestUtils.prepareNetCatProcess( Proto.TCP, true, port );
        process = new ProcessBuilder( listener ).redirectErrorStream( true ).start();

        // Execute connect operation after some delay
        Thread.sleep( 500 );
        connect( port );

        send();
        receive();
    }

    @Test
    public void testTcpListen() throws IOException, InterruptedException
    {
        int port = 9997;

        // Connect to NetCat by external nc after some delay required for NetCat listener to start
        final List<String> dialer = TestUtils.prepareNetCatProcess( Proto.TCP, false, port );
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

        send();
        receive();
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

    public Closeable connect( int port ) throws InterruptedException
    {
        netCat.execute( CONNECT.toString(), Proto.TCP.toString(), TestUtils.HOST, String.valueOf( port ) );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertNull( result.exception );
        assertThat( result.op, is( CONNECT ));
        assertNotNull( result.getSocket() );
        return result.getSocket();
    }

    public Closeable listen( int port ) throws InterruptedException
    {
        netCat.execute( LISTEN.toString(), Proto.TCP.toString(), String.valueOf( port ) );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertNull( result.exception );
        assertThat( result.op, is( LISTEN ));
        assertNotNull( result.getSocket() );
        return result.getSocket();
    }

    public void disconnect() throws InterruptedException
    {
        netCat.execute( DISCONNECT.toString() );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertNull( result.exception );
        assertThat( result.op, is( DISCONNECT ));
    }

    public void send() throws InterruptedException, IOException
    {
        // Send string to external nc process
        netCat.setInput( new ByteArrayInputStream( inputFromTest.getBytes() ) );
        netCat.execute( SEND.toString() );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertEquals( SEND, result.op );

        // Wait till nc process will be started definitely
        while( process == null ) {
            Thread.sleep( 100 );
        }

        // Get received string by external nc process
        BufferedReader b = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
        String line;
        do {
            line = b.readLine();
            Log.i( CLASS_NAME, line );
        } while( !inputFromTest.equals( line ) );
    }

    public void receive() throws IOException, InterruptedException
    {
        // Wait till nc process will be started definitely
        while( process == null ) {
            Thread.sleep( 100 );
        }

        // Send string from external nc process
        process.getOutputStream().write( inputFromProcess.getBytes() );
        process.getOutputStream().flush();
        process.getOutputStream().close();

        // Receive string from external nc process
        netCat.createOutput();
        netCat.execute( RECEIVE.toString() );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertThat( result.op, is( RECEIVE ));
        String line = netCat.getOutputString();
        Log.i( CLASS_NAME, line  );
        assertThat( line, is( inputFromProcess ));
    }
}
