package com.github.dddpaul.netcat;

import android.util.Log;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.robolectric.shadows.ShadowLog;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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

public abstract class NetCatTestParent extends Assert
{
    final static String INPUT_TEST = "Input from this test, привет, €, 汉语";
    final static String INPUT_NC = "Input from netcat process, пока, £, 语汉";
    final static String HOST = "localhost";
    final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    protected NetCater netCat;
    protected Result result;
    protected CountDownLatch latch;
    protected Process process;
    protected String inputFromTest, inputFromProcess;

    @BeforeClass
    public static void init()
    {
        ShadowLog.stream = System.out;
    }

    public List<String> prepareNetCatProcess( Proto proto, boolean listen, int port )
    {
        List<String> result = new ArrayList<>();
        result.add( "nc" );
        result.add( "-v" );
        if( proto == Proto.UDP ) {
            result.add( "-u" );
        }
        if( listen ) {
            result.add( "-l" );
        } else {
            result.add( HOST );
        }
        result.add( String.valueOf( port ) );
        return result;
    }

    public void connect( int port ) throws InterruptedException
    {
        netCat.execute( CONNECT.toString(), HOST, String.valueOf( port ) );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertNull( result.exception );
        assertThat( result.op, is( CONNECT ));
        assertNotNull( result.getSocket() );
    }

    public void listen( int port ) throws InterruptedException
    {
        netCat.execute( LISTEN.toString(), String.valueOf( port ) );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertNull( result.exception );
        assertThat( result.op, is( LISTEN ));
        assertNotNull( result.getSocket() );
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
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    process.getOutputStream().write( inputFromProcess.getBytes() );
                    process.getOutputStream().flush();
                    if( netCat instanceof UdpNetCat ) {
                        Thread.sleep( 500 );
                        process.getOutputStream().write( UdpNetCat.DISCONNECT_SEQUENCE.getBytes() );
                        process.getOutputStream().flush();
                    }
                    process.getOutputStream().close();
                } catch( Exception e ) {
                    e.printStackTrace();
                }
            }
        } ).start();

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
