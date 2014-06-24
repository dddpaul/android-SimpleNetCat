package com.github.dddpaul.netcat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class ResultFragment extends Fragment
{
    @InjectView( R.id.result )
    protected TextView resultView;

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
        return view;
    }
}
