package com.github.dddpaul.netcat;

import java.util.ArrayList;
import java.util.List;

import static com.github.dddpaul.netcat.NetCater.Proto;

public class TestUtils
{
    final static String INPUT_TEST = "Input from this test, привет, €, 汉语";
    final static String INPUT_NC = "Input from netcat process, пока, £, 语汉";
    final static String HOST = "localhost";

    public static List<String> prepareNetCatProcess( Proto proto, boolean listen, int port )
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
}
