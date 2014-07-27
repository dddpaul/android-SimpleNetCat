package com.github.dddpaul.netcat.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.dddpaul.netcat.NetCatListener;
import com.github.dddpaul.netcat.NetCatModule;
import com.github.dddpaul.netcat.NetCater;
import com.github.dddpaul.netcat.R;
import com.github.dddpaul.netcat.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import dagger.ObjectGraph;
import de.greenrobot.event.EventBus;
import events.ActivityEvent;
import events.FragmentEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;

import javax.inject.Inject;

import static com.github.dddpaul.netcat.NetCater.Op.*;
import static com.github.dddpaul.netcat.NetCater.Result;
import static com.github.dddpaul.netcat.NetCater.State.*;

public class ResultFragment extends Fragment implements NetCatListener
{
    private final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    private ByteArrayOutputStream output;

    @Inject
    protected NetCater netCat;

    @InjectView( R.id.et_input )
    protected EditText inputText;

    @InjectView( R.id.tv_output )
    protected TextView outputView;

    @InjectView( R.id.b_send )
    protected Button sendButton;

    public ResultFragment() {}

    public static ResultFragment newInstance()
    {
        return new ResultFragment();
    }

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setRetainInstance( true );
        EventBus.getDefault().register( this );
        ObjectGraph.create( new NetCatModule() ).inject( this );
        netCat.setListener( this );
    }

    @Override
    public void onDestroy()
    {
        EventBus.getDefault().unregister( this );
        super.onDestroy();
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
        return view;
    }

    @OnClick( R.id.b_send )
    protected void onSendButtonClick()
    {
        send();
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
            case LISTEN:
                Socket socket = result.getSocket();
                output = new ByteArrayOutputStream();
                netCat.setSocket( socket );
                netCat.setOutput( output );
                netCat.executeParallel( RECEIVE.toString() );
                EventBus.getDefault().post( new ActivityEvent( CONNECTED ) );
                break;
            case RECEIVE:
                // Strip last CR+LF
                String s = output.toString();
                outputView.setText( s.substring( 0, s.length() - 1 ));
                disconnect();
                break;
            case SEND:
                inputText.setText( "" );
                break;
            case DISCONNECT:
                Toast.makeText( getActivity(), "Connection is closed", Toast.LENGTH_LONG ).show();
                EventBus.getDefault().post( new ActivityEvent( IDLE, outputView.getText().toString() ) );
                break;
        }
        updateUIWithValidation();
    }

    @Override
    public void netCatIsFailed( Result result )
    {
        Toast.makeText( getActivity(), result.getErrorMessage(), Toast.LENGTH_LONG ).show();
    }

    public void onEvent( FragmentEvent event )
    {
        switch( event.op ) {
            case CONNECT:
                connect( event.data );
                break;
            case LISTEN:
                listen( event.data );
                break;
            case DISCONNECT:
                if( netCat.isConnected() ) {
                    disconnect();
                } else if( netCat.isListening() ) {
                    netCat.cancel();
                }
                break;
            case CLEAR_OUTPUT_VIEW:
                outputView.setText( "" );
                EventBus.getDefault().post( new ActivityEvent( OUTPUT_VIEW_CLEARED ) );
                break;
        }
    }

    /**
     * For unit test only
     */
    public void setNetCat( NetCater netCat )
    {
        this.netCat = netCat;
    }

    public void connect( String connectTo )
    {
        if( Utils.isActive( netCat )) {
            Toast.makeText( getActivity(), getString( R.string.error_disconnect_first ), Toast.LENGTH_LONG ).show();
            return;
        }
        if( !connectTo.matches( "[\\w\\.]+:\\d+" ) ) {
            Toast.makeText( getActivity(), getString( R.string.error_host_port_format ), Toast.LENGTH_LONG ).show();
            return;
        }
        String[] tokens = connectTo.split( ":" );
        netCat.execute( CONNECT.toString(), tokens[0], tokens[1] );
    }

    public void listen( String port )
    {
        if( Utils.isActive( netCat )) {
            Toast.makeText( getActivity(), getString( R.string.error_disconnect_first ), Toast.LENGTH_LONG ).show();
            return;
        }
        if( !port.matches( "\\d+" ) ) {
            Toast.makeText( getActivity(), getString( R.string.error_port_format ), Toast.LENGTH_LONG ).show();
            return;
        }
        netCat.execute( LISTEN.toString(), port );
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
        if( Utils.isActive( netCat ) ) {
            sendButton.setEnabled( Utils.populated( inputText ) );
        } else {
            sendButton.setEnabled( false );
        }
    }
}
