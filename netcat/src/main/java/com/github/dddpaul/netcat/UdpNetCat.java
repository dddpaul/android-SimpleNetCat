package com.github.dddpaul.netcat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;

import static com.github.dddpaul.netcat.NetCater.State.CONNECTED;

public class UdpNetCat extends NetCat
{
    public static final String DISCONNECT_SEQUENCE = "~.";

    private final String CLASS_NAME = getClass().getSimpleName();

    private NetCatTask task;
    private DatagramChannel channel;

    public UdpNetCat( NetCatListener listener )
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
        return channel != null && channel.isOpen();
    }

    @Override
    public boolean isConnected()
    {
        return channel != null && channel.isConnected();
    }

    public class NetCatTask extends Task
    {
        @Override
        protected Result doInBackground( String... params )
        {
            Op op = Op.valueOf( params[0] );
            Result result = new Result( op, Proto.UDP );
            try {
                Log.i( CLASS_NAME, String.format( "Executing %s operation (UDP)", op ) );
                int port;
                switch( op ) {
                    case CONNECT:
                        String host = params[1];
                        port = Integer.parseInt( params[2] );
                        channel = DatagramChannel.open();
                        channel.connect( new InetSocketAddress( host, port ) );
                        channel.configureBlocking( false );
                        Log.d( CLASS_NAME, String.format( "Connected to %s (UDP)", channel.socket().getRemoteSocketAddress() ) );
                        result.object = channel.socket();
                        break;
                    case LISTEN:
                        port = Integer.parseInt( params[1] );
                        Log.d( CLASS_NAME, String.format( "Listening on %d (UDP)", port ) );
                        channel = DatagramChannel.open();
                        channel.socket().bind( new InetSocketAddress( port ) );
                        channel.configureBlocking( false );
                        result.object = channel.socket();
                        break;
                    case RECEIVE:
                        if( isListening() ) {
                            receiveFromChannel();
                        }
                        break;
                    case SEND:
                        if( isConnected() ) {
                            sendToChannel();
                        }
                        break;
                    case DISCONNECT:
                        if( isConnected() ) {
                            disconnect();
                        } else if( isListening() ) {
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

        private void receiveFromChannel() throws Exception
        {
            SocketAddress remoteSocketAddress = null;
            int bytesReceived = 0;
            try {
                ByteBuffer buf = ByteBuffer.allocate( Constants.RECEIVE_BUFFER_LIMIT );
                buf.clear();
                while( !task.isCancelled() ) {
                    if( isListening() && !isConnected() ) {
                        remoteSocketAddress = channel.receive( buf );
                        bytesReceived = buf.position() - 1;
                    }
                    if( isConnected() ) {
                        bytesReceived = channel.read( buf );
                    }
                    if( bytesReceived > 0 ) {
                        Log.d( CLASS_NAME, String.format( "%d bytes was received from %s (UDP)", bytesReceived, remoteSocketAddress ) );
                        if( DISCONNECT_SEQUENCE.equals( new String( buf.array(), 0, buf.position() ).trim() ) ) {
                            Log.d( CLASS_NAME, String.format( "Disconnect sequence was received from %s (UDP)", remoteSocketAddress ) );
                            break;
                        }
                        output.write( buf.array(), 0, buf.position() );
                        publishProgress( CONNECTED.toString(), output.toString() );
                        buf.clear();
                        // Connect after receive is necessary for further sending
                        if( !isConnected() ) {
                            channel.connect( remoteSocketAddress );
                            Log.d( CLASS_NAME, String.format( "Connected to %s (UDP)", channel.socket().getRemoteSocketAddress() ) );
                        }
                    }
                    Thread.sleep( 100 );
                }
                if( task.isCancelled() ) {
                    stopListening();
                    throw new Exception( "Listening task is cancelled" );
                }
            } catch( AsynchronousCloseException e ) {
                // This exception is thrown when socket for receiver thread is closed by netcat
                Log.w( CLASS_NAME, e.toString() );
            }
        }

        private void sendToChannel() throws IOException
        {
            byte[] buf = new byte[Constants.SEND_BUFFER_LIMIT];
            int bytesRead = input.read( buf, 0, buf.length );
            if( bytesRead > 0 ) {
                int bytesSent = channel.send( ByteBuffer.wrap( buf, 0, bytesRead ), channel.socket().getRemoteSocketAddress() );
                Log.d( CLASS_NAME, String.format( "%d bytes was sent to %s (UDP)", bytesSent, channel.socket().getRemoteSocketAddress() ) );
            }
        }

        private void disconnect() throws IOException
        {
            Log.d( CLASS_NAME, String.format( "Disconnecting from %s (UDP)", channel.socket().getRemoteSocketAddress() ) );
            channel.close();
            channel = null;
        }

        private void stopListening()
        {
            Log.d( CLASS_NAME, String.format( "Stop listening on %d (UDP)", channel.socket().getLocalPort() ) );
            try {
                channel.close();
            } catch( IOException e ) {
                e.printStackTrace();
            }
            channel = null;
        }
    }
}
