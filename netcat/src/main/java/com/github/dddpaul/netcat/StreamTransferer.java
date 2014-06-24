package com.github.dddpaul.netcat;

import java.io.*;

public class StreamTransferer implements Runnable
{
    private InputStream input;
    private OutputStream output;

    public StreamTransferer( InputStream input, OutputStream output )
    {
        this.input = input;
        this.output = output;
    }

    @Override
    public void run()
    {
        try {
            PrintWriter writer = new PrintWriter( output );
            BufferedReader reader = new BufferedReader( new InputStreamReader( input ) );
            String line;
            while( ( line = reader.readLine() ) != null ) {
                writer.println( line );
                writer.flush();
            }
        } catch( IOException e ) {
            e.printStackTrace();
        }
    }
}
