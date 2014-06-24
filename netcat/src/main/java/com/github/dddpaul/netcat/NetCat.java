package com.github.dddpaul.netcat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

public class NetCat
{
    private final String CLASS_NAME = getClass().getSimpleName();
    private NetCatListener listener;

    public void setListener( NetCatListener listener )
    {
        this.listener = listener;
    }

    public void execute( String ... params)
    {
        new NetCatTask().execute( params );
    }

    public class NetCatTask extends AsyncTask<String, Void, Void>
    {
        @Override
        protected void onPreExecute()
        {
            listener.netCatIsStarted();
        }

        @Override
        protected Void doInBackground( String... params )
        {
            String host = params[0];
            int port = Integer.parseInt( params[1] );
            try {
                Log.i( CLASS_NAME, String.format( "Connecting to %s:%d", host, port ));
                Socket socket = new Socket( host, port );
                transferStreams( socket );
            } catch( Exception e ) {
                Log.e( CLASS_NAME, e.getMessage() );
                listener.netCatIsFailed( e );
            }
            return null;
        }

        @Override
        protected void onPostExecute( Void aVoid )
        {
            listener.netCatIsCompleted();
        }

        private void transferStreams( Socket socket ) throws IOException, InterruptedException
        {
            InputStream input1 = System.in;
            OutputStream output1 = socket.getOutputStream();
            InputStream input2 = socket.getInputStream();
            PrintStream output2 = System.out;

            Thread thread1 = new Thread( new StreamTransferer( input1, output1 ) );
            thread1.setName( "Thread1: Local-Remote" );

            Thread thread2 = new Thread( new StreamTransferer( input2, output2 ) );
            thread2.setName( "Thread2: Remote-Local" );

            thread1.start();
            thread2.start();

            // Wait till other side is terminated
            thread2.join();
        }
    }
}
