package com.github.dddpaul.netcat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import events.ActivityEvent;

import static com.github.dddpaul.netcat.NetCater.State.*;

public class NetCat implements NetCater
{
    private final String CLASS_NAME = getClass().getSimpleName();

    private NetCatTask task;
    private NetCatListener listener;
    private ServerSocketChannel serverChannel;
    private Socket socket;
    private InputStream input;
    private OutputStream output;

    /**
     * Needs for NetCatModule
     */
    @Inject
    public NetCat() {}

    public NetCat( NetCatListener listener )
    {
        this.listener = listener;
    }

    @Override
    public void setListener( NetCatListener listener )
    {
        this.listener = listener;
    }

    @Override
    public void setSocket( Socket socket )
    {
        this.socket = socket;
    }

    @Override
    public void setInput( InputStream input )
    {
        this.input = input;
    }

    @Override
    public void setOutput( OutputStream output )
    {
        this.output = output;
    }

    @Override
    public void cancel()
    {
        if( task != null ) {
            task.cancel( false );
        }
    }

    @Override
    public void execute( String... params )
    {
        task = new NetCatTask();
        task.execute( params );
    }

    @Override
    public void executeParallel( String... params )
    {
        task = new NetCatTask();
        task.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, params );
    }

    @Override
    public boolean isConnected()
    {
        return socket != null && socket.isConnected();
    }

    @Override
    public boolean isListening()
    {
        return serverChannel != null;
    }

    public class NetCatTask extends AsyncTask<String, String, Result>
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
                Log.d( CLASS_NAME, String.format( "Executing %s operation", op ) );
                int port;
                Socket newSocket;
                switch( op ) {
                    case CONNECT:
                        String host = params[1];
                        port = Integer.parseInt( params[2] );
                        Log.d( CLASS_NAME, String.format( "Connecting to %s:%d", host, port ) );
                        newSocket = new Socket();
                        newSocket.connect( new InetSocketAddress( host, port ), 3000 );
                        publishProgress( CONNECTED.toString() );
                        result.object = newSocket;
                        break;
                    case LISTEN:
                        port = Integer.parseInt( params[1] );
                        serverChannel = ServerSocketChannel.open();
                        serverChannel.configureBlocking( false );
                        serverChannel.socket().bind( new InetSocketAddress( port ) );
                        Log.d( CLASS_NAME, String.format( "Listening on %d", port ) );
                        publishProgress( LISTENING.toString() );
                        while( !task.isCancelled() ) {
                            SocketChannel channel = serverChannel.accept();
                            Thread.sleep( 100 );
                            if( channel != null ) {
                                result.object = channel.socket();
                                publishProgress( CONNECTED.toString() );
                                break;
                            }
                        }
                        if( task.isCancelled() ) {
                            serverChannel.close();
                            serverChannel = null;
                            Log.d( CLASS_NAME, String.format( "Stop listening on %d", port ) );
                            result.exception = new Exception( "Listening task is cancelled" );
                        }
                        break;
                    case RECEIVE:
                        if( socket != null && socket.isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Receiving from %s:%d",
                                    socket.getInetAddress().getHostAddress(), socket.getPort() ) );
                            receiveFromSocket();
                        }
                        break;
                    case SEND:
                        if( socket != null && socket.isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Sending to %s:%d",
                                    socket.getInetAddress().getHostAddress(), socket.getPort() ) );
                            sendToSocket();
                        }
                        break;
                    case DISCONNECT:
                        if( socket != null && socket.isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Disconnecting from %s:%d",
                                    socket.getInetAddress().getHostAddress(), socket.getPort() ) );
                            socket.shutdownOutput();
                            socket.close();
                            socket = null;
                            publishProgress( IDLE.toString() );
                        }
                }
            } catch( Exception e ) {
                e.printStackTrace();
                result.exception = e;
            }
            return result;
        }

        @Override
        protected void onProgressUpdate( String... values )
        {
            State state = State.valueOf( String.valueOf( values[0] ) );
            EventBus.getDefault().post( new ActivityEvent( state ) );
        }

        @Override
        protected void onPostExecute( Result result )
        {
            if( result.exception == null ) {
                Log.d( CLASS_NAME, String.format( "%s operation is completed", result.op ) );
                listener.netCatIsCompleted( result );
            } else {
                Log.e( CLASS_NAME, result.getErrorMessage() );
                listener.netCatIsFailed( result );
            }
        }

        @Override
        protected void onCancelled( Result result )
        {
            EventBus.getDefault().post( new ActivityEvent( IDLE ) );
            listener.netCatIsFailed( result );
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
    }
}
