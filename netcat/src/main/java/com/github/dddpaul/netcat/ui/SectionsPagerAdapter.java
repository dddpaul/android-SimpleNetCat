package com.github.dddpaul.netcat.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.github.dddpaul.netcat.R;

import java.util.Locale;

public class SectionsPagerAdapter extends FragmentPagerAdapter
{
    private MainActivity activity;

    public SectionsPagerAdapter( MainActivity mainActivity, FragmentManager fm )
    {
        super( fm );
        this.activity = mainActivity;
    }

    @Override
    public Fragment getItem( int position )
    {
        switch( position ) {
            case 0:
                return MainFragment.newInstance();
            case 1:
                return ResultFragment.newInstance();
            default:
                return null;
        }
    }

    @Override
    public float getPageWidth( int position )
    {
        if( activity.isMultiPaneLayout() ) {
            switch( position ) {
                case 0:
                    return 0.4f;
                case 1:
                    return 0.6f;
                default:
                    return 1f;
            }
        } else {
            return 1f;
        }
    }

    @Override
    public int getCount()
    {
        return 2;
    }

    @Override
    public CharSequence getPageTitle( int position )
    {
        Locale l = Locale.getDefault();
        switch( position ) {
            case 0:
                return activity.getString( R.string.title_section1 ).toUpperCase( l );
            case 1:
                return activity.getString( R.string.title_section2 ).toUpperCase( l );
        }
        return null;
    }
}
