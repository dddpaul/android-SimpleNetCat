package com.github.dddpaul.netcat;

import android.util.Log;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAsyncTask;
import org.robolectric.shadows.ShadowLog;

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
import static com.github.dddpaul.netcat.NetCater.Result;

@Config( emulateSdk = 18 )
@RunWith( RobolectricTestRunner.class )
public class NetCatTest extends Assert implements NetCatListener
{
    final static String INPUT_TEST = "Input from this test";
    final static String INPUT_NC = "Input from netcat process";
    final static String HOST = "localhost";
    final static String PORT = "9999";
    final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    static List<String> nc = new ArrayList<>();

    ShadowAsyncTask<String, Void, Result> shadowTask;
    NetCat netCat;
    Result result;
    CountDownLatch latch;
    Process process;

    /**
     * Require netcat-openbsd package for Debian/Ubuntu
     */
    @BeforeClass
    public static void init()
    {
        nc.add( "nc" );
        nc.add( "-v" );
        nc.add( "-l" );
        nc.add( PORT );
    }

    @Before
    public void setUp() throws Exception
    {
        process = new ProcessBuilder( nc ).redirectErrorStream( true ).start();
        ShadowLog.stream = System.out;
        netCat = new NetCat( this );
        NetCat.NetCatTask task = netCat.new NetCatTask();
        shadowTask = Robolectric.shadowOf( task );
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

    @Test
    public void test() throws InterruptedException, IOException
    {
        Socket socket = connect();
        netCat.setSocket( socket );

        // Send string to nc process
        netCat.setInput( new ByteArrayInputStream( INPUT_TEST.getBytes() ));
        shadowTask.execute( SEND.toString() );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertEquals( SEND, result.op );

        // Get received string by nc process
        BufferedReader b = new BufferedReader( new InputStreamReader( process.getInputStream() ));
        String line;
        do {
            line = b.readLine();
            Log.i( CLASS_NAME, line  );

        } while( !INPUT_TEST.equals( line ));

        // Send string from nc process
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

        // Prepare to receive string from nc process
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        netCat.setOutput( output );
        shadowTask.execute( RECEIVE.toString() );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertEquals( RECEIVE, result.op );
        line = new String( output.toByteArray() ).trim();
        Log.i( CLASS_NAME, line  );
        assertEquals( INPUT_NC, line );

        disconnect();
    }

    public Socket connect() throws InterruptedException
    {
        shadowTask.execute( CONNECT.toString(), HOST, PORT );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertNull( result.exception );
        assertEquals( CONNECT, result.op );
        assertNotNull( result.getSocket() );
        return result.getSocket();
    }

    public void disconnect() throws InterruptedException
    {
        shadowTask.execute( DISCONNECT.toString() );
        latch.await( 5, TimeUnit.SECONDS );

        assertNotNull( result );
        assertNull( result.exception );
        assertEquals( DISCONNECT, result.op );
    }
}
