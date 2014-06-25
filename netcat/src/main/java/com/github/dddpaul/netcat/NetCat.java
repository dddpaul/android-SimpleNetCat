package com.github.dddpaul.netcat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.*;
import java.net.Socket;

public class NetCat
{
    public enum Op { CONNECT, LISTEN, SEND, RECEIVE }

    private final String CLASS_NAME = getClass().getSimpleName();

    private NetCatListener listener;
    private Socket socket;
    private Op op;
    private InputStream input;
    private OutputStream output;
    private Exception exception;

    public NetCat( OutputStream output )
    {
        this.output = output;
    }

    public void setListener( NetCatListener listener )
    {
        this.listener = listener;
    }

    public void execute( String ... params )
    {
        new NetCatTask().execute( params );
    }

    public class NetCatTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected void onPreExecute()
        {
            listener.netCatIsStarted();
        }

        @Override
        protected String doInBackground( String... params )
        {
            try {
                op = Op.valueOf( params[0] );
                switch( op ) {
                    case CONNECT:
                        String host = params[1];
                        int port = Integer.parseInt( params[2] );
                        Log.i( CLASS_NAME, String.format( "Connecting to %s:%d", host, port ) );
                        socket = new Socket( host, port );
                        break;
                    case RECEIVE:
                        if( socket != null && socket.isConnected() ) {
                            transferStreams( socket );
                        }
                        break;
                }
            } catch( Exception e ) {
                exception = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute( String result )
        {
            if( exception == null ) {
                Log.i( CLASS_NAME, String.format( "%s task is completed", op ));
                listener.netCatIsCompleted( op );
            } else {
                Log.e( CLASS_NAME, exception.getMessage() );
                listener.netCatIsFailed( exception );
            }
        }

        private void transferStreams( Socket socket ) throws IOException, InterruptedException
        {
            InputStream input = socket.getInputStream();
            PrintWriter writer = new PrintWriter( output );
            BufferedReader reader = new BufferedReader( new InputStreamReader( input ) );
            String line;
            while( ( line = reader.readLine() ) != null ) {
                writer.println( line );
                writer.flush();
            }
        }

/*
        private void transferStreams( Socket socket ) throws IOException, InterruptedException
        {
            InputStream input1 = System.in;
            OutputStream output1 = socket.getOutputStream();
            InputStream input2 = socket.getInputStream();
            PrintStream output2 = new PrintStream( output );

            Thread thread1 = new Thread( new StreamTransferer( input1, output1 ) );
            thread1.setName( "Thread1: Local-Remote" );

            Thread thread2 = new Thread( new StreamTransferer( input2, output2 ) );
            thread2.setName( "Thread2: Remote-Local" );

            thread1.start();
            thread2.start();

            // Wait till other side is terminated
            thread2.join();
        }
*/
    }
}
