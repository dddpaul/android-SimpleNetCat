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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ResultFragment extends Fragment implements NetCatListener
{
    private final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    private ByteArrayOutputStream output;

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

    @Override
    public void onResume()
    {
        super.onResume();
        updateUIWithValidation();
    }

    public void startNetCat( String connectTo )
    {
        // TODO: Make some validation
        String[] tokens = connectTo.split( ":" );
        output = new ByteArrayOutputStream();
        NetCat netCat = new NetCat( output );
        netCat.setListener( this );
        netCat.execute( tokens );
    }

    @Override
    public void netCatIsStarted() {}

    @Override
    public void netCatIsCompleted()
    {
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
            Log.i( CLASS_NAME, "Data is written to result text view" );
        } catch( IOException e ) {
            Log.e( CLASS_NAME, e.getMessage() );
        }
    }

    @Override
    public void netCatIsFailed( Exception e )
    {
        Toast.makeText( getActivity(), e.getMessage(), Toast.LENGTH_LONG ).show();
    }

    private void updateUIWithValidation()
    {
        boolean populated = Utils.populated( inputText );
        sendButton.setEnabled( populated );
    }
}
