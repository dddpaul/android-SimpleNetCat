package com.github.dddpaul.netcat;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends ActionBarActivity implements ActionBar.TabListener, NetCatListener
{
    private final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    SectionsPagerAdapter adapter;
    ViewPager pager;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        adapter = new SectionsPagerAdapter( this, getSupportFragmentManager() );

        // Set up the ViewPager with the sections adapter.
        pager = (ViewPager) findViewById( R.id.pager );
        pager.setAdapter( adapter );

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        pager.setOnPageChangeListener( new ViewPager.SimpleOnPageChangeListener()
        {
            @Override
            public void onPageSelected( int position )
            {
                actionBar.setSelectedNavigationItem( position );
            }
        } );

        // For each of the sections in the app, add a tab to the action bar.
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate( R.menu.main, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if( id == R.id.action_settings ) {
            return true;
        }
        return super.onOptionsItemSelected( item );
    }

    @Override
    public void onTabSelected( ActionBar.Tab tab, FragmentTransaction fragmentTransaction )
    {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
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

    public void startNetCat( String connectTo )
    {
        // OutputStream to TextView in ResultFragment
        OutputStream output = new OutputStream()
        {
            @Override
            public void write( int oneByte ) throws IOException
            {
                TextView textView = (TextView) findViewById( R.id.result );
                char ch = (char) oneByte;
                textView.setText( textView.getText() + String.valueOf( ch ) );
                System.out.write( oneByte );
            }
        };

        // TODO: Make some validation
        String[] tokens = connectTo.split( ":" );
        NetCat netCat = new NetCat( output );
        netCat.setListener( this );
        netCat.execute( tokens );
        pager.setCurrentItem( 1 );
    }

    @Override
    public void netCatIsStarted() {}

    @Override
    public void netCatIsCompleted() {}

    @Override
    public void netCatIsFailed( Exception e )
    {
        Log.e( CLASS_NAME, e.getMessage() );
    }
}
