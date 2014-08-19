package com.github.dddpaul.netcat.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.dddpaul.netcat.Constants;
import com.github.dddpaul.netcat.R;
import com.github.dddpaul.netcat.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import events.FragmentEvent;

import static com.github.dddpaul.netcat.NetCater.Op.*;
import static com.github.dddpaul.netcat.NetCater.Proto;

public class MainFragment extends Fragment
{
    @InjectView( R.id.et_connect_to )
    protected AutoCompleteTextView connectToText;

    @InjectView( R.id.b_connect )
    protected Button connectButton;

    @InjectView( R.id.et_listen_on )
    protected EditText listenOnText;

    @InjectView( R.id.b_listen )
    protected Button listenButton;

    @InjectView( R.id.c_tcp_udp )
    protected CheckBox udpCheckbox;

    @InjectView( R.id.tv_udp_tooltip )
    protected TextView udpTooltip;

    private SharedPreferences prefs;
    private Set<String> connectToSet;
    private ArrayAdapter<String> connectToArrayAdapter;

    public static MainFragment newInstance()
    {
        return new MainFragment();
    }

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        prefs = getActivity().getSharedPreferences( Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE );
        connectToSet = prefs.getStringSet( Constants.CONNECT_TO_SET_KEY, new HashSet<String>() );
        connectToArrayAdapter = new ArrayAdapter<>( getActivity(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>( connectToSet ));
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
        connectToText.setAdapter( connectToArrayAdapter );
        connectToText.addTextChangedListener( watcher );
        listenOnText.addTextChangedListener( watcher );
        return view;
    }

    @OnClick( R.id.b_connect )
    protected void onConnectButtonClick()
    {
        String connectTo = connectToText.getText().toString();
        if( connectToSet.add( connectTo )) {
            connectToArrayAdapter.add( connectTo );
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet( Constants.CONNECT_TO_SET_KEY, connectToSet );
        editor.apply();

        Proto proto = udpCheckbox.isChecked() ? Proto.UDP : Proto.TCP;
        connectTo = proto + ":" + connectTo;
        EventBus.getDefault().post( new FragmentEvent( CONNECT, connectTo ) );
    }

    @OnClick( R.id.b_listen )
    protected void onListenButtonClick()
    {
        Proto proto = udpCheckbox.isChecked() ? Proto.UDP : Proto.TCP;
        String listenOn = proto + ":" + listenOnText.getText().toString();
        EventBus.getDefault().post( new FragmentEvent( LISTEN, listenOn ) );
    }

    @OnCheckedChanged( R.id.c_tcp_udp )
    protected void onUdpChecked( boolean checked )
    {
        if( checked ) {
            udpTooltip.setVisibility( View.VISIBLE );
        } else {
            udpTooltip.setVisibility( View.INVISIBLE );
        }
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
