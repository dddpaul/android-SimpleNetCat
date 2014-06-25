package com.github.dddpaul.netcat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.*;
import java.net.Socket;

public class NetCat
{
    public enum Op { CONNECT, LISTEN, RECEIVE, SEND }

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

    public void setInput( InputStream input )
    {
        this.input = input;
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
                Log.d( CLASS_NAME, String.format( "Executing %s operation", op ));
                switch( op ) {
                    case CONNECT:
                        String host = params[1];
                        int port = Integer.parseInt( params[2] );
                        Log.d( CLASS_NAME, String.format( "Connecting to %s:%d", host, port ) );
                        socket = new Socket( host, port );
                        break;
                    case RECEIVE:
                        Log.d( CLASS_NAME, "Socket connected = " + String.valueOf( socket.isConnected() ));
                        if( socket != null && socket.isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Receiving from %s:%d",
                                    socket.getInetAddress().getHostAddress(), socket.getPort() ) );
                            receiveFromSocket();
                        }
                        break;
                    case SEND:
                        Log.d( CLASS_NAME, "Socket connected = " + String.valueOf( socket.isConnected() ));
                        if( socket != null && socket.isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Sending to %s:%d",
                                    socket.getInetAddress().getHostAddress(), socket.getPort() ) );
                            sendToSocket();
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
                Log.d( CLASS_NAME, String.format( "%s operation is completed", op ));
                listener.netCatIsCompleted( op );
            } else {
                Log.e( CLASS_NAME, exception.getMessage() );
                listener.netCatIsFailed( exception );
            }
        }

        private void receiveFromSocket() throws IOException
        {
            BufferedReader reader = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
            PrintWriter writer = new PrintWriter( output );
            transferStreams( reader, writer );
        }

        private void sendToSocket() throws IOException
        {
            BufferedReader reader = new BufferedReader( new InputStreamReader( input ) );
            PrintWriter writer = new PrintWriter( socket.getOutputStream() );
            transferStreams( reader, writer );
        }

        private void transferStreams( BufferedReader reader, PrintWriter writer ) throws IOException
        {
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
