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

import com.github.dddpaul.netcat.R;
import com.github.dddpaul.netcat.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import events.FragmentEvent;

import static com.github.dddpaul.netcat.NetCater.Op.*;

public class MainFragment extends Fragment
{
    @InjectView( R.id.et_connect_to )
    protected EditText connectToText;

    @InjectView( R.id.b_connect )
    protected Button connectButton;

    @InjectView( R.id.et_listen_on )
    protected EditText listenOnText;

    @InjectView( R.id.b_listen )
    protected Button listenButton;

    public static MainFragment newInstance()
    {
        return new MainFragment();
    }

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
    {
        View view = inflater.inflate( R.layout.fragment_main, container, false );
        ButterKnife.inject( this, view );
        TextWatcher watcher = new TextWatcherAdapter()
        {
            public void afterTextChanged( final Editable editable )
            {
                updateUIWithValidation();
            }
        };
        connectToText.addTextChangedListener( watcher );
        connectButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View v )
            {
                EventBus.getDefault().post( new FragmentEvent( CONNECT, connectToText.getText().toString() ) );
            }
        } );
        listenOnText.addTextChangedListener( watcher );
        listenButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View v )
            {
                EventBus.getDefault().post( new FragmentEvent( LISTEN, listenOnText.getText().toString() ) );
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

    private void updateUIWithValidation()
    {
        connectButton.setEnabled( Utils.populated( connectToText ) );
        listenButton.setEnabled( Utils.populated( listenOnText ) );
    }
}
