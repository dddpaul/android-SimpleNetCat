package com.github.dddpaul.netcat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainFragment extends Fragment
{
    protected NetCat netCat;

    @InjectView( R.id.et_host )
    protected EditText hostText;

    @InjectView( R.id.b_start )
    protected Button startBtn;

    public MainFragment() {}

    public static MainFragment newInstance()
    {
        return new MainFragment();
    }

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        netCat = new NetCat();
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
        hostText.addTextChangedListener( watcher );
        startBtn.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View v )
            {
                startNetCat( hostText.getText().toString() );
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

    private boolean populated( final EditText editText )
    {
        return editText.length() > 0;
    }

    private void updateUIWithValidation()
    {
        boolean populated = populated( hostText );
        startBtn.setEnabled( populated );
    }

    private void startNetCat( String connectTo )
    {
        // TODO: Make some validation
        String[] tokens = connectTo.split( ":" );
        netCat.execute( tokens );
    }
}
