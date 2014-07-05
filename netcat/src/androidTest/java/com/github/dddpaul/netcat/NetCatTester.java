package com.github.dddpaul.netcat;

import android.util.Log;

import org.junit.Assert;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.dddpaul.netcat.NetCater.Op.*;
import static org.hamcrest.core.Is.is;

public abstract class NetCatTester extends Assert implements NetCatListener
{
    final static String INPUT_TEST = "Input from this test";
    final static String INPUT_NC = "Input from netcat process";
    final static String HOST = "localhost";
    final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    static List<String> nc = new ArrayList<>();

    NetCater netCat;
    NetCater.Result result;
    CountDownLatch latch;
    Process process;

    @Override
    public void netCatIsStarted()
    {
        latch = new CountDownLatch( 1 );
    }

    @Override
    public void netCatIsCompleted( NetCater.Result result )
    {
        this.result = result;
        latch.countDown();
    }

    @Override
    public void netCatIsFailed( NetCater.Result result )
    {
        this.result = result;
        Log.e( CLASS_NAME, result.getErrorMessage() );
        latch.countDown();
    }

    public Socket connect( String port ) throws InterruptedException
    {
        netCat.execute( CONNECT.toString(), HOST, port );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertNull( result.exception );
        assertThat( result.op, is( CONNECT ));
        assertNotNull( result.getSocket() );
        return result.getSocket();
    }

    public Socket listen( String port ) throws InterruptedException
    {
        netCat.execute( LISTEN.toString(), port );
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

    public void testNetCatOperations() throws InterruptedException, IOException
    {
        // Send string to external nc process
        netCat.setInput( new ByteArrayInputStream( INPUT_TEST.getBytes() ));
        netCat.execute( SEND.toString() );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertEquals( SEND, result.op );

        // Get received string by external nc process
        BufferedReader b = new BufferedReader( new InputStreamReader( process.getInputStream() ));
        String line;
        do {
            line = b.readLine();
            Log.i( CLASS_NAME, line  );
        } while( !INPUT_TEST.equals( line ));

        // Send string from external nc process
        process.getOutputStream().write( INPUT_NC.getBytes() );
        process.getOutputStream().flush();
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    Thread.sleep( 500 );
                } catch( Exception e ) {
                    e.printStackTrace();
                }
                process.destroy();
            }
        }).start();

        // Receive string from external nc process
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        netCat.setOutput( output );
        netCat.execute( RECEIVE.toString() );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertThat( result.op, is( RECEIVE ));
        line = new String( output.toByteArray() ).trim();
        Log.i( CLASS_NAME, line  );
        assertThat( line, is( INPUT_NC ));

        disconnect();
    }
}
