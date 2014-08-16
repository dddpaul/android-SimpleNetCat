package com.github.dddpaul.netcat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.AsynchronousCloseException;
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
    private Closeable socket;
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
    public void setInput( InputStream input )
    {
        this.input = input;
    }

    @Override
    public void createOutput()
    {
        this.output = new ByteArrayOutputStream();
    }

    @Override
    public void closeOutput()
    {
        try {
            output.flush();
            output.close();
        } catch( IOException e ) {
            Log.e( CLASS_NAME, e.getMessage() );
        }
        output = null;
    }

    @Override
    public OutputStream getOutput()
    {
        return output;
    }

    /**
     * Strip last CR+LF
     */
    @Override
    public String getOutputString()
    {
        String s = output.toString();
        if( s.endsWith( "\n" )) {
            s = s.substring( 0, s.length() - 1 );
        }
        return s;
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
        if( socket != null ) {
            if( socket instanceof Socket ) return ( (Socket) socket ).isConnected();
            if( socket instanceof DatagramSocket ) return ( (DatagramSocket) socket ).isConnected();
        }
        return false;
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
                switch( op ) {
                    case CONNECT:
                        Proto proto = Proto.valueOf( params[1] );
                        String host = params[2];
                        port = Integer.parseInt( params[3] );
                        Log.d( CLASS_NAME, String.format( "Connecting to %s:%d (%s)", host, port, proto ) );
                        if( proto == Proto.TCP ) {
                            socket = new Socket();
                            ( (Socket) socket).connect( new InetSocketAddress( host, port ), 3000 );
                        } else {
                            socket = new DatagramSocket();
                            ( (DatagramSocket) socket).connect( new InetSocketAddress( host, port ) );
                        }
                        publishProgress( CONNECTED.toString() );
                        result.object = socket;
                        break;
                    case LISTEN:
                        proto = Proto.valueOf( params[1] );
                        port = Integer.parseInt( params[2] );
                        serverChannel = ServerSocketChannel.open();
                        serverChannel.configureBlocking( false );
                        serverChannel.socket().bind( new InetSocketAddress( port ) );
                        Log.d( CLASS_NAME, String.format( "Listening on %d (%s)", port, proto ) );
                        publishProgress( LISTENING.toString() );
                        while( !task.isCancelled() ) {
                            SocketChannel channel = serverChannel.accept();
                            Thread.sleep( 100 );
                            if( channel != null ) {
                                socket = channel.socket();
                                result.object = channel.socket();
                                publishProgress( CONNECTED.toString() );
                                break;
                            }
                        }
                        if( task.isCancelled() ) {
                            stopListening( port );
                            result.exception = new Exception( "Listening task is cancelled" );
                        }
                        break;
                    case RECEIVE:
                        if( isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Receiving from %s", getSocketString( socket )));
                            if( socket instanceof Socket ) {
                                receiveFromSocket( (Socket) socket );
                            } else {
                                receiveFromDatagramSocket( (DatagramSocket) socket );
                            }
                        }
                        break;
                    case SEND:
                        if( isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Sending to %s", getSocketString( socket )));
                            if( socket instanceof Socket ) {
                                sendToSocket( (Socket) socket );
                            } else {
                                sendToDatagramSocket( (DatagramSocket) socket );
                            }
                        }
                        break;
                    case DISCONNECT:
                        if( serverChannel != null ) {
                            stopListening( serverChannel.socket().getLocalPort() );
                        }
                        if( isConnected() ) {
                            Log.d( CLASS_NAME, String.format( "Disconnecting from %s", getSocketString( socket )));
                            if( socket instanceof Socket ) {
                                ( (Socket) socket).shutdownOutput();
                            }
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

        private void receiveFromSocket( Socket socket) throws IOException
        {
            BufferedReader reader = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
            PrintWriter writer = new PrintWriter( output );
            transferStreams( reader, writer );
        }

        private void sendToSocket( Socket socket ) throws IOException
        {
            BufferedReader reader = new BufferedReader( new InputStreamReader( input ) );
            PrintWriter writer = new PrintWriter( socket.getOutputStream() );
            transferStreams( reader, writer );
        }

        private void transferStreams( BufferedReader reader, PrintWriter writer ) throws IOException
        {
            try {
                String line;
                while( ( line = reader.readLine() ) != null ) {
                    writer.println( line );
                    writer.flush();
                }
            } catch( AsynchronousCloseException e ) {
                // This exception is thrown when socket for receiver thread is closed by netcat
                Log.w( CLASS_NAME, e.toString() );
            }
        }

        private void receiveFromDatagramSocket( DatagramSocket socket ) throws IOException
        {
            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket( buf, buf.length );
            socket.receive( packet );
            output.write( packet.getData(), 0, packet.getLength() );
        }

        private void sendToDatagramSocket( DatagramSocket socket ) throws IOException
        {
            BufferedReader reader = new BufferedReader( new InputStreamReader( input ) );
            char[] buf = new char[1024];
            reader.read( buf, 0, 1024 );
            String line = new String( buf );
            DatagramPacket packet = new DatagramPacket( line.getBytes(), line.length() );
            socket.send( packet );
        }

        private void stopListening( int port ) throws IOException
        {
            Log.d( CLASS_NAME, String.format( "Stop listening on %d", port ) );
            serverChannel.close();
            serverChannel = null;
        }

        private String getSocketString( Closeable socket )
        {
            if( socket instanceof Socket ) {
                Socket tcpSocket = (Socket) socket;
                return tcpSocket.getInetAddress().getHostAddress() + ":" + tcpSocket.getPort();

            }
            if( socket instanceof DatagramSocket ) {
                DatagramSocket udpSocket = (DatagramSocket) socket;
                return udpSocket.getInetAddress().getHostAddress() + ":" + udpSocket.getPort();

            }
            return "";
        }
    }
}
