package com.github.dddpaul.netcat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;

import static com.github.dddpaul.netcat.NetCater.State.CONNECTED;

public class UdpNetCat extends NetCat
{
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
                Log.d( CLASS_NAME, String.format( "Executing %s operation", op ) );
                int port;
                switch( op ) {
                    case CONNECT:
                        String host = params[1];
                        port = Integer.parseInt( params[2] );
                        Log.d( CLASS_NAME, String.format( "Connecting to %s:%d (UDP)", host, port ) );
                        channel = DatagramChannel.open();
                        channel.connect( new InetSocketAddress( host, port ) );
                        channel.configureBlocking( false );
                        Log.i( CLASS_NAME, "Connected to " + channel.socket().getRemoteSocketAddress() );
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
                            SocketAddress remoteSocketAddress = receiveFromChannel();
                            if( remoteSocketAddress != null ) {
                                // Connect after receive is necessary for further sending
                                Log.d( CLASS_NAME, String.format( "Received data from %s (UDP)", remoteSocketAddress ) );
                                channel.connect( remoteSocketAddress );
                                Log.d( CLASS_NAME, String.format( "Connected to %s (UDP)", channel.socket().getRemoteSocketAddress() ) );
                            }
                        }
                        break;
                    case SEND:
                        if( isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Sending to %s (UDP)", channel.socket().getRemoteSocketAddress() ) );
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
                e.printStackTrace();
                result.exception = e;
            }
            return result;
        }

        private SocketAddress receiveFromChannel() throws Exception
        {
            SocketAddress remoteSocketAddress = null;
            try {
                ByteBuffer buf = ByteBuffer.allocate( 1024 );
                buf.clear();
                while( !task.isCancelled() ) {
                    int oldPosition = buf.position();
                    remoteSocketAddress = channel.receive( buf );
                    if( remoteSocketAddress != null ) {
                        Log.d( CLASS_NAME, String.format( "%d bytes was received from %s", buf.position() - oldPosition - 1, remoteSocketAddress ));
                        output.write( buf.array(), oldPosition, buf.position() );
                        publishProgress( CONNECTED.toString(), output.toString() );
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
            return remoteSocketAddress;
        }

        private void sendToChannel() throws IOException
        {
            byte[] buf = new byte[1024];
            int bytesRead = input.read( buf, 0, buf.length );
            if( bytesRead > 0 ) {
                channel.send( ByteBuffer.wrap( buf ), channel.socket().getRemoteSocketAddress() );
            }
        }

        private void disconnect() throws IOException
        {
            Log.d( CLASS_NAME, String.format( "Disconnecting from %s (UDP)", channel.socket().getRemoteSocketAddress() ) );
            channel.close();
            channel = null;
        }

        private void stopListening() throws IOException
        {
            Log.d( CLASS_NAME, String.format( "Stop listening on %d (UDP)", channel.socket().getLocalPort() ) );
            channel.close();
            channel = null;
        }
    }
}
