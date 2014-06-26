package com.github.dddpaul.netcat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.*;
import java.net.Socket;

import static com.github.dddpaul.netcat.NetCater.*;

public class NetCat
{
    private final String CLASS_NAME = getClass().getSimpleName();

    private NetCatListener listener;
    private Socket socket;
    private InputStream input;
    private OutputStream output;

    public NetCat( NetCatListener listener )
    {
        this.listener = listener;
    }

    public void setSocket( Socket socket )
    {
        this.socket = socket;
    }

    public void setInput( InputStream input )
    {
        this.input = input;
    }

    public void setOutput( OutputStream output )
    {
        this.output = output;
    }

    public void execute( String ... params )
    {
        // Serial execution
        //new NetCatTask().execute( params );
        // Parallel execution
        new NetCatTask().executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, params );
    }

    public class NetCatTask extends AsyncTask<String, Void, Result>
    {
        @Override
        protected void onPreExecute()
        {
            listener.netCatIsStarted();
        }

        @Override
        protected Result doInBackground( String... params )
        {
            Op op = Op.valueOf( params[0] );
            Result result = new Result( op );
            try {
                Log.d( CLASS_NAME, String.format( "Executing %s operation", op ));
                switch( op ) {
                    case CONNECT:
                        String host = params[1];
                        int port = Integer.parseInt( params[2] );
                        Log.d( CLASS_NAME, String.format( "Connecting to %s:%d", host, port ) );
                        result.object = new Socket( host, port );
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
                result.exception = e;
            }
            return result;
        }

        @Override
        protected void onPostExecute( Result result )
        {
            if( result.exception == null ) {
                Log.d( CLASS_NAME, String.format( "%s operation is completed", result.op ));
                listener.netCatIsCompleted( result );
            } else {
                Log.e( CLASS_NAME, result.getErrorMessage() );
                listener.netCatIsFailed( result );
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
