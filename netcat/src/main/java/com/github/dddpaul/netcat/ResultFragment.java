package com.github.dddpaul.netcat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import static com.github.dddpaul.netcat.NetCater.Op.*;
import static com.github.dddpaul.netcat.NetCater.Result;

public class ResultFragment extends Fragment implements NetCatListener
{
    private final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    private ByteArrayOutputStream output;
    private NetCater netCat;

    @InjectView( R.id.et_input )
    protected EditText inputText;

    @InjectView( R.id.tv_output )
    protected TextView outputView;

    @InjectView( R.id.b_send )
    protected Button sendButton;

    @InjectView( R.id.b_disconnect )
    protected Button disconnectButton;

    public ResultFragment() {}

    public static ResultFragment newInstance()
    {
        return new ResultFragment();
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
    {
        View view = inflater.inflate( R.layout.fragment_result, container, false );
        ButterKnife.inject( this, view );
        TextWatcher watcher = new TextWatcherAdapter()
        {
            public void afterTextChanged( final Editable editable )
            {
                updateUIWithValidation();
            }
        };
        inputText.addTextChangedListener( watcher );
        sendButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View v )
            {
                send();
            }
        } );
        disconnectButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View v )
            {
                disconnect();
            }
        } );
        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        updateUIWithValidation();
    }

    @Override
    public void netCatIsStarted() {}

    @Override
    public void netCatIsCompleted( Result result )
    {
        switch( result.op ) {
            case CONNECT:
                Socket socket = result.getSocket();
                output = new ByteArrayOutputStream();
                netCat.setSocket( socket );
                netCat.setOutput( output );
                netCat.execute( RECEIVE.toString() );
                updateUIWithValidation();
                break;
            case RECEIVE:
                // OutputStream to TextView in ResultFragment
                OutputStream resultStream = new OutputStream()
                {
                    @Override
                    public void write( int oneByte ) throws IOException
                    {
                        char ch = (char) oneByte;
                        outputView.setText( outputView.getText() + String.valueOf( ch ) );
                        System.out.write( oneByte );
                    }
                };

                try {
                    output.writeTo( resultStream );
                } catch( IOException e ) {
                    Log.e( CLASS_NAME, e.getMessage() );
                }
                break;
            case DISCONNECT:
                Toast.makeText( getActivity(), "Connection is closed", Toast.LENGTH_LONG ).show();
                updateUIWithValidation();
                break;
        }
    }

    @Override
    public void netCatIsFailed( Result result )
    {
        Toast.makeText( getActivity(), result.getErrorMessage(), Toast.LENGTH_LONG ).show();
    }

    public void connect( String connectTo )
    {
        // TODO: Make some validation
        String[] tokens = connectTo.split( ":" );
        netCat = new NetCat( this );
        netCat.execute( CONNECT.toString(), tokens[0], tokens[1] );
    }

    private void send()
    {
        byte[] bytes = inputText.getText().toString().getBytes();
        ByteArrayInputStream input = new ByteArrayInputStream( bytes );
        netCat.setInput( input );
        netCat.execute( SEND.toString() );
    }

    private void disconnect()
    {
        netCat.execute( DISCONNECT.toString() );
    }

    private void updateUIWithValidation()
    {
        if( Utils.connected( netCat )) {
            sendButton.setEnabled( Utils.populated( inputText ) );
            disconnectButton.setEnabled( true );
        } else {
            disconnectButton.setEnabled( false );
        }
    }
}
