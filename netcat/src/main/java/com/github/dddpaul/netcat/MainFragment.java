package com.github.dddpaul.netcat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainFragment extends Fragment
{
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
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
    {
        View view = inflater.inflate( R.layout.fragment_main, container, false );
        ButterKnife.inject( this, view );
        startBtn.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View v )
            {
                startNetcat( hostText.getText().toString() );
            }
        } );
        return view;
    }

    private void startNetcat( String host )
    {
        Toast.makeText( getActivity(), host, Toast.LENGTH_LONG );
    }
}
