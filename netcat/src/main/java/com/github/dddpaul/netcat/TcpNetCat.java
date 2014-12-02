package com.github.dddpaul.netcat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static com.github.dddpaul.netcat.NetCater.State.CONNECTED;
import static com.github.dddpaul.netcat.NetCater.State.IDLE;
import static com.github.dddpaul.netcat.NetCater.State.LISTENING;

public class TcpNetCat extends NetCat
{
    private final String CLASS_NAME = getClass().getSimpleName();

    private NetCatTask task;
    private ServerSocketChannel serverChannel;
    private Socket socket;

    public TcpNetCat( NetCatListener listener )
    {
        super( listener );
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
    public boolean isListening()
    {
        return serverChannel != null;
    }

    @Override
    public boolean isConnected()
    {
        return socket != null && socket.isConnected();
    }

    public class NetCatTask extends Task
    {
        @Override
        protected Result doInBackground( String... params )
        {
            Op op = Op.valueOf( params[0] );
            Result result = new Result( op, Proto.TCP );
            try {
                Log.d( CLASS_NAME, String.format( "Executing %s operation", op ) );
                int port;
                switch( op ) {
                    case CONNECT:
                        String host = params[1];
                        port = Integer.parseInt( params[2] );
                        Log.d( CLASS_NAME, String.format( "Connecting to %s:%d (TCP)", host, port ) );
                        socket = new Socket();
                        socket.connect( new InetSocketAddress( host, port ), 3000 );
                        result.object = socket;
                        break;
                    case LISTEN:
                        port = Integer.parseInt( params[1] );
                        Log.d( CLASS_NAME, String.format( "Listening on %d (TCP)", port ) );
                        serverChannel = ServerSocketChannel.open();
                        serverChannel.configureBlocking( false );
                        serverChannel.socket().bind( new InetSocketAddress( port ) );
                        publishProgress( LISTENING.toString() );
                        while( !task.isCancelled() ) {
                            SocketChannel channel = serverChannel.accept();
                            Thread.sleep( 100 );
                            if( channel != null ) {
                                socket = channel.socket();
                                result.object = socket;
                                break;
                            }
                        }
                        if( task.isCancelled() ) {
                            stopListening();
                            result.exception = new Exception( "Listening task is cancelled" );
                        }
                        break;
                    case RECEIVE:
                        if( isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Receiving from %s (TCP)", socket.getRemoteSocketAddress() ) );
                            receiveFromSocket();
                        }
                        break;
                    case SEND:
                        if( isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Sending to %s (TCP)", socket.getRemoteSocketAddress() ) );
                            sendToSocket();
                        }
                        break;
                    case DISCONNECT:
                        if( isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Disconnecting from %s (TCP)", socket.getRemoteSocketAddress() ) );
                            socket.shutdownOutput();
                            socket.close();
                            socket = null;
                        }
                        if( isListening() ) {
                            stopListening();
                        }
                        break;
                }
            } catch( Exception e ) {
                if( e instanceof BindException ) {
                    stopListening();
                }
                e.printStackTrace();
                result.exception = e;
            }
            return result;
        }

        private void receiveFromSocket() throws IOException
        {
            BufferedReader reader = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
            PrintWriter writer = new PrintWriter( output );
            transferStreams( reader, writer, true );
        }

        private void sendToSocket() throws IOException
        {
            BufferedReader reader = new BufferedReader( new InputStreamReader( input ) );
            PrintWriter writer = new PrintWriter( socket.getOutputStream() );
            transferStreams( reader, writer, false );
        }

        private void transferStreams( BufferedReader reader, PrintWriter writer, boolean receive ) throws IOException
        {
            try {
                String line;
                while( ( line = reader.readLine() ) != null ) {
                    writer.println( line );
                    writer.flush();
                    if( receive ) {
                        publishProgress( CONNECTED.toString(), output.toString() );
                    }
               }
            } catch( AsynchronousCloseException e ) {
                // This exception is thrown when socket for receiver thread is closed by netcat
                Log.w( CLASS_NAME, e.toString() );
            }
        }

        private void stopListening()
        {
            Log.d( CLASS_NAME, String.format( "Stop listening on %d (TCP)", serverChannel.socket().getLocalPort() ) );
            try {
                serverChannel.close();
            } catch( IOException e ) {
                e.printStackTrace();
            }
            serverChannel = null;
        }
    }
}
