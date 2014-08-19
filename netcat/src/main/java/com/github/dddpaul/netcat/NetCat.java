package com.github.dddpaul.netcat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.*;

import de.greenrobot.event.EventBus;
import events.ActivityEvent;
import events.FragmentEvent;

import static com.github.dddpaul.netcat.NetCater.Op.*;

public class NetCat implements NetCater
{
    private final String CLASS_NAME = getClass().getSimpleName();

    protected NetCatListener listener;
    protected InputStream input;
    protected OutputStream output;

    public NetCat( NetCatListener listener )
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
    public void cancel() {}

    @Override
    public void execute( String... params ) {}

    @Override
    public void executeParallel( String... params ) {}

    @Override
    public boolean isListening()
    {
        return false;
    }

    @Override
    public boolean isConnected()
    {
        return false;
    }

    public class Task extends AsyncTask<String, String, Result>
    {
        @Override
        protected void onPreExecute()
        {
            listener.netCatIsStarted();
        }

        @Override
        protected Result doInBackground( String... params )
        {
            return null;
        }

        @Override
        protected void onProgressUpdate( String... values )
        {
            State state = State.valueOf( String.valueOf( values[0] ) );
            EventBus.getDefault().post( new ActivityEvent( state ) );
            if( values.length == 2 ) {
                EventBus.getDefault().post( new FragmentEvent( HANDLE_RECEIVED_DATA, values[1] ));
            }
        }

        @Override
        protected void onPostExecute( Result result )
        {
            if( result.exception == null ) {
                Log.i( CLASS_NAME, String.format( "%s operation (%s) is completed", result.op, result.proto ) );
                listener.netCatIsCompleted( result );
            } else {
                Log.e( CLASS_NAME, result.getErrorMessage() );
                listener.netCatIsFailed( result );
            }
        }

        @Override
        protected void onCancelled( Result result )
        {
            listener.netCatIsFailed( result );
        }
    }
}
