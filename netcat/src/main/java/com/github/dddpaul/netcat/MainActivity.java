package com.github.dddpaul.netcat;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity implements ActionBar.TabListener, OnFragmentInteractionListener
{
    private final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    SectionsPagerAdapter adapter;
    ViewPager pager;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        // Set up the action bar
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );

        // Set up the ViewPager with the sections adapter
        adapter = new SectionsPagerAdapter( this, getSupportFragmentManager() );
        pager = (ViewPager) findViewById( R.id.pager );
        pager.setAdapter( adapter );
        pager.setOnPageChangeListener( new ViewPager.SimpleOnPageChangeListener()
        {
            @Override
            public void onPageSelected( int position )
            {
                actionBar.setSelectedNavigationItem( position );
            }
        } );

        // For each of the sections in the app, add a tab to the action bar
        for( int i = 0; i < adapter.getCount(); i++ ) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText( adapter.getPageTitle( i ) )
                            .setTabListener( this ) );
        }
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.main, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        int id = item.getItemId();
        if( id == R.id.action_settings ) {
            return true;
        }
        return super.onOptionsItemSelected( item );
    }

    @Override
    public void onTabSelected( ActionBar.Tab tab, FragmentTransaction fragmentTransaction )
    {
        pager.setCurrentItem( tab.getPosition() );
    }

    @Override
    public void onTabUnselected( ActionBar.Tab tab, FragmentTransaction fragmentTransaction )
    {
    }

    @Override
    public void onTabReselected( ActionBar.Tab tab, FragmentTransaction fragmentTransaction )
    {
    }

    @Override
    public void onFragmentInteraction( int position )
    {
        pager.setCurrentItem( position, false );
    }

    @Override
    public void onFragmentInteraction( int position, String connectTo )
    {
        if( position == getResources().getInteger( R.integer.result_fragment_position ) ) {
            ResultFragment fragment = (ResultFragment) adapter.getRegisteredFragment( position );
            fragment.connect( connectTo );
        }
    }
}
