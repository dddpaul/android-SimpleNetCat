package com.github.dddpaul.netcat.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.dddpaul.netcat.NetCatListener;
import com.github.dddpaul.netcat.NetCater;
import com.github.dddpaul.netcat.R;
import com.github.dddpaul.netcat.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import events.ActivityEvent;
import events.FragmentEvent;

import java.io.ByteArrayInputStream;

import static com.github.dddpaul.netcat.Constants.NETCAT_FRAGMENT_TAG;
import static com.github.dddpaul.netcat.Constants.RECEIVED_TEXT_KEY;
import static com.github.dddpaul.netcat.NetCater.*;
import static com.github.dddpaul.netcat.NetCater.Op.*;
import static com.github.dddpaul.netcat.NetCater.Result;
import static com.github.dddpaul.netcat.NetCater.State.*;

public class ResultFragment extends Fragment implements NetCatListener
{
    private final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    private NetCater netCat;
    private NetCatFragment netCatFragment;
    private TextWatcherAdapter watcher;

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
        EventBus.getDefault().register( this );
    }

    @Override
    public void onDestroy()
    {
        inputText.removeTextChangedListener( watcher );
        sendButton.setOnClickListener( null );
        EventBus.getDefault().unregister( this );
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState( Bundle outState )
    {
        super.onSaveInstanceState( outState );
        outState.putString( RECEIVED_TEXT_KEY, outputView.getText().toString() );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
    {
        View view = inflater.inflate( R.layout.fragment_result, container, false );
        ButterKnife.inject( this, view );
        watcher = createTextWatcherAdapter();
        inputText.addTextChangedListener( watcher );
        outputView.setMovementMethod( new ScrollingMovementMethod() );
        if( savedInstanceState != null ) {
            outputView.setText( savedInstanceState.getString( RECEIVED_TEXT_KEY, "" ));
        }
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
        netCatFragment = (NetCatFragment) getFragmentManager().findFragmentByTag( NETCAT_FRAGMENT_TAG );
        if( netCatFragment != null ) {
            netCat = netCatFragment.getNetCat();
        }
    }

    @Override
    public void netCatIsStarted() {}

    @Override
    public void netCatIsCompleted( Result result )
    {
        switch( result.op ) {
            case CONNECT:
            case LISTEN:
                netCat.createOutput();
                netCat.executeParallel( RECEIVE.toString() );
                State state = result.proto == Proto.TCP ? CONNECTED :
                        result.op == CONNECT ? CONNECTING : LISTENING;
                EventBus.getDefault().post( new ActivityEvent( state ) );
                break;
            case RECEIVE:
                outputView.setText( netCat.getOutputString() );
                netCat.closeOutput();
                if( netCat.isConnected() ) {
                    disconnect();
                }
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
        EventBus.getDefault().post( new ActivityEvent( IDLE, outputView.getText().toString() ) );
        Toast.makeText( getActivity(), result.getErrorMessage(), Toast.LENGTH_LONG ).show();
    }

    /**
     * Event handler
     */
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
            case HANDLE_RECEIVED_DATA:
                outputView.setText( event.data );
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
    public void setNetCatFragment( NetCatFragment netCatFragment )
    {
        this.netCatFragment = netCatFragment;
    }

    public void connect( String connectTo )
    {
        if( Utils.isActive( netCat )) {
            Toast.makeText( getActivity(), getString( R.string.error_disconnect_first ), Toast.LENGTH_LONG ).show();
            return;
        }
        if( !connectTo.matches( "(TCP|UDP):[\\w\\.]+:\\d+" ) ) {
            Toast.makeText( getActivity(), getString( R.string.error_host_port_format ), Toast.LENGTH_LONG ).show();
            return;
        }
        String[] tokens = connectTo.split( ":" );
        netCat = netCatFragment.getOrCreateNetCat( Proto.valueOf( tokens[0] ), this );
        netCat.execute( CONNECT.toString(), tokens[1], tokens[2] );
    }

    public void listen( String listenOn )
    {
        if( Utils.isActive( netCat )) {
            Toast.makeText( getActivity(), getString( R.string.error_disconnect_first ), Toast.LENGTH_LONG ).show();
            return;
        }
        if( !listenOn.matches( "(TCP|UDP):\\d+" ) ) {
            Toast.makeText( getActivity(), getString( R.string.error_port_format ), Toast.LENGTH_LONG ).show();
            return;
        }
        String[] tokens = listenOn.split( ":" );
        netCat = netCatFragment.getOrCreateNetCat( Proto.valueOf( tokens[0] ), this );
        netCat.execute( LISTEN.toString(), tokens[1] );
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
        if( netCat != null && netCat.isConnected() ) {
            sendButton.setEnabled( Utils.populated( inputText ) );
        } else {
            sendButton.setEnabled( false );
        }
    }

    private TextWatcherAdapter createTextWatcherAdapter()
    {
        return new TextWatcherAdapter()
        {
            public void afterTextChanged( final Editable editable )
            {
                updateUIWithValidation();
            }
        };
    }
}
