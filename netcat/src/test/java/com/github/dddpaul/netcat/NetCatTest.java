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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.dddpaul.netcat.NetCater.*;
import static com.github.dddpaul.netcat.NetCater.Op.CONNECT;
import static com.github.dddpaul.netcat.NetCater.Op.DISCONNECT;
import static com.github.dddpaul.netcat.NetCater.Op.LISTEN;
import static com.github.dddpaul.netcat.NetCater.Op.RECEIVE;
import static com.github.dddpaul.netcat.NetCater.Op.SEND;
import static org.hamcrest.core.Is.is;

@Config( emulateSdk = 18 )
@RunWith( RobolectricTestRunner.class )
public class NetCatTest extends Assert implements NetCatListener
{
    final static String INPUT_TEST = "Input from this test, привет, €, 汉语";
    final static String INPUT_NC = "Input from netcat process, пока, £, 语汉";
    final static String HOST = "localhost";
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
        netCat = new NetCat( this );
        inputFromTest = INPUT_TEST;
        inputFromProcess = INPUT_NC;
    }

    @After
    public void tearDown() throws InterruptedException
    {
        disconnect();
        process.destroy();
    }

    @Test
    public void testTCPConnect() throws IOException, InterruptedException
    {
        startConnectTest( Proto.TCP );
    }

    @Test
    public void testTCPListen() throws IOException, InterruptedException
    {
        startListenTest( Proto.TCP );
    }

    @Test
    public void testUDPConnect() throws IOException, InterruptedException
    {
        inputFromTest = inputFromTest + "\n";
        inputFromProcess = inputFromProcess + "\n";
        startConnectTest( Proto.UDP );
    }

    @Test
    public void testUDPListen() throws IOException, InterruptedException
    {
        inputFromTest = inputFromTest + "\n";
        inputFromProcess = inputFromProcess + "\n";
        startListenTest( Proto.UDP );
    }

    public void startConnectTest( Proto proto ) throws InterruptedException, IOException
    {
        String port = "9998";

        // Start external nc listener
        List<String> nc = new ArrayList<>();
        nc.add( "nc" );
        nc.add( "-v" );
        if( proto == Proto.UDP ) {
            nc.add( "-u" );
        }
        nc.add( "-l" );
        nc.add( port );
        process = new ProcessBuilder( nc ).redirectErrorStream( true ).start();

        // Execute connect operation after some delay
        Thread.sleep( 500 );
        connect( proto, port );

        send();
        receive();
    }

    public void startListenTest( Proto proto ) throws InterruptedException, IOException
    {
        String port = "9997";

        // Connect to NetCat by external nc after some delay required for NetCat listener to start
        final List<String> nc = new ArrayList<>();
        nc.add( "nc" );
        nc.add( "-v" );
        if( proto == Proto.UDP ) {
            nc.add( "-u" );
        }
        nc.add( HOST );
        nc.add( port );
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    Thread.sleep( 500 );
                    process = new ProcessBuilder( nc ).redirectErrorStream( true ).start();
                } catch( Exception e ) {
                    e.printStackTrace();
                }
            }
        } ).start();

        // Start NetCat listener
        listen( proto, port );

        if( proto == Proto.TCP ) {
            send();
            receive();
        } else {
            // UDP listener must wait for receive to move into connected state then send
            receive();
            send();
        }
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

    public Closeable connect( Proto proto, String port ) throws InterruptedException
    {
        netCat.execute( CONNECT.toString(), proto.toString(), HOST, port );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertNull( result.exception );
        assertThat( result.op, is( CONNECT ));
        assertNotNull( result.getSocket() );
        return result.getSocket();
    }

    public Closeable listen( Proto proto, String port ) throws InterruptedException
    {
        netCat.execute( LISTEN.toString(), proto.toString(), port );
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
        } while( !INPUT_TEST.equals( line ) );
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
        assertThat( line, is( INPUT_NC ));
    }
}
