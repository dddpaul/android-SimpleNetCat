package com.github.dddpaul.netcat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.github.dddpaul.netcat.NetCater;
import com.github.dddpaul.netcat.R;
import com.github.dddpaul.netcat.Utils;

import de.greenrobot.event.EventBus;
import events.ActivityEvent;
import events.FragmentEvent;

public class MainActivity extends ActionBarActivity implements ActionBar.TabListener
{
    private final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    private Menu menu;
    private ViewPager pager;
    private TextView statusView;
    private ShareActionProvider shareProvider;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        // Set up the action bar
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );

        // Set up the ViewPager with the sections adapter
        SectionsPagerAdapter adapter = new SectionsPagerAdapter( this, getSupportFragmentManager() );
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
                            .setTabListener( this )
            );
        }

        EventBus.getDefault().register( this );
    }

    @Override
    public void onDestroy()
    {
        EventBus.getDefault().unregister( this );
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.actions, menu );
        this.menu = menu;
        MenuItem statusItem = menu.findItem( R.id.action_status );
        statusView = (TextView) MenuItemCompat.getActionView( statusItem );
        MenuItem shareItem = menu.findItem( R.id.action_share );
        shareProvider = (ShareActionProvider) MenuItemCompat.getActionProvider( shareItem );
        return super.onCreateOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() ) {
            case R.id.action_settings:
                return true;
            case R.id.action_cancel:
                EventBus.getDefault().post( new FragmentEvent( NetCater.Op.DISCONNECT ) );
                break;
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

    public void onEvent( ActivityEvent event )
    {
        switch( event.netCatState ) {
            case CONNECTED:
            case LISTENING:
                statusView.setText( event.netCatState.toString() );
                setDisconnectButtonVisibility( true );
                pager.setCurrentItem( getResources().getInteger( R.integer.result_fragment_position ), false );
                break;
            case IDLE:
                statusView.setText( "" );
                setDisconnectButtonVisibility( false );
                if( Utils.isNotEmpty( event.text )) {
                    shareProvider.setShareIntent( getShareIntent( event.text ) );
                }
                break;

        }
    }

    public void setDisconnectButtonVisibility( boolean visible )
    {
        menu.findItem( R.id.action_cancel ).setVisible( visible );
        onPrepareOptionsMenu( menu );
    }

    private Intent getShareIntent( String text )
    {
        Intent intent = new Intent();
        intent.setAction( Intent.ACTION_SEND );
        intent.putExtra( Intent.EXTRA_TEXT, text );
        intent.setType( "text/plain" );
        return Intent.createChooser( intent, "Title" );
    }
}
