package com.github.dddpaul.netcat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.IOException;
import java.net.Socket;

@Config( emulateSdk = 18 )
@RunWith( RobolectricTestRunner.class )
public class NetCatConnectTest extends NetCatTester
{
    final static String PORT = "9999";

    /**
     * Require nc binary (netcat-openbsd package for Debian/Ubuntu).
     * nc is used to LISTEN for connect from our NetCat.
     */
    @Before
    public void setUp() throws Exception
    {
        nc.add( "nc" );
        nc.add( "-v" );
        nc.add( "-l" );
        nc.add( PORT );

        // Start external nc listener
        process = new ProcessBuilder( nc ).redirectErrorStream( true ).start();

        ShadowLog.stream = System.out;
        netCat = new NetCat( this );
    }

    @Test
    public void test() throws InterruptedException, IOException
    {
        // Execute connect operation after some delay
        Thread.sleep( 500 );
        Socket socket = connect( PORT );
        netCat.setSocket( socket );

        testNetCatOperations();
    }
}
