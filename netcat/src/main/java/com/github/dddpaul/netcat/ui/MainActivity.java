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

import com.github.dddpaul.netcat.R;
import com.github.dddpaul.netcat.Utils;

import de.greenrobot.event.EventBus;
import events.ActivityEvent;
import events.FragmentEvent;

import static com.github.dddpaul.netcat.Constants.ACTIONS_VISIBILITY_KEY;
import static com.github.dddpaul.netcat.Constants.NETCAT_FRAGMENT_TAG;
import static com.github.dddpaul.netcat.Constants.NETCAT_STATE_KEY;
import static com.github.dddpaul.netcat.NetCater.*;

public class MainActivity extends ActionBarActivity implements ActionBar.TabListener
{
    private final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    private Menu menu;
    private MenuItem cancelItem, clearItem, shareItem, statusItem;
    private ViewPager pager;
    private TextView stateView;
    private ShareActionProvider shareProvider;
    private boolean[] actionsVisibility;
    private State netCatState;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        EventBus.getDefault().register( this );

        // Instantiate headless retained fragment for the first time init
        if( savedInstanceState == null ) {
            FragmentTransaction trx = getSupportFragmentManager().beginTransaction();
            trx.add( NetCatFragment.newInstance(), NETCAT_FRAGMENT_TAG );
            trx.commit();
        } else {
            actionsVisibility = savedInstanceState.getBooleanArray( ACTIONS_VISIBILITY_KEY );
            try {
                netCatState = State.valueOf( savedInstanceState.getString( NETCAT_STATE_KEY ) );
            } catch( IllegalArgumentException e ) {
                netCatState = null;
            }
        }

        // Set up the ViewPager with the sections adapter
        pager = (ViewPager) findViewById( R.id.pager );
        SectionsPagerAdapter adapter = new SectionsPagerAdapter( this, getSupportFragmentManager() );
        pager.setAdapter( adapter );

        if( !isMultiPaneLayout() ) {
            // Set up the action bar
            final ActionBar actionBar = getSupportActionBar();
            actionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );

            // For each of the sections in the app, add a tab to the action bar
            for( int i = 0; i < adapter.getCount(); i++ ) {
                actionBar.addTab(
                        actionBar.newTab()
                                .setText( adapter.getPageTitle( i ) )
                                .setTabListener( this )
                );
            }

            pager.setOnPageChangeListener( new ViewPager.SimpleOnPageChangeListener()
            {
                @Override
                public void onPageSelected( int position )
                {
                    actionBar.setSelectedNavigationItem( position );
                }
            } );
        }
    }

    @Override
    public void onDestroy()
    {
        EventBus.getDefault().unregister( this );
        pager.removeAllViews();   // Prevent fragment leaking on 4.1.x,
        pager.setAdapter( null ); // it's fixed in 4.3
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState( Bundle outState )
    {
        super.onSaveInstanceState( outState );
        outState.putBooleanArray( ACTIONS_VISIBILITY_KEY, getActionsVisibility() );
        outState.putString( NETCAT_STATE_KEY, stateView.getText().toString() );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.actions, menu );
        this.menu = menu;
        cancelItem = menu.findItem( R.id.action_cancel );
        clearItem = menu.findItem( R.id.action_clear );
        shareItem = menu.findItem( R.id.action_share );
        shareProvider = (ShareActionProvider) MenuItemCompat.getActionProvider( shareItem );
        statusItem = menu.findItem( R.id.action_state );
        stateView = (TextView) MenuItemCompat.getActionView( statusItem );
        if( actionsVisibility != null ) {
            setActionsVisibility( actionsVisibility );
        }
        if( netCatState != null ) {
            stateView.setText( netCatState.toString() );
        }
        return super.onCreateOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() ) {
            case R.id.action_cancel:
                EventBus.getDefault().post( new FragmentEvent( Op.DISCONNECT ) );
                break;
            case R.id.action_clear:
                EventBus.getDefault().post( new FragmentEvent( Op.CLEAR_OUTPUT_VIEW ) );
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
            case CONNECTING:
                cancelItem.setVisible( true );
                shareItem.setVisible( false );
                statusItem.setVisible( true );
                stateView.setText( event.netCatState.toString() );
                if( pager != null ) {
                    pager.setCurrentItem( getResources().getInteger( R.integer.result_fragment_position ), false );
                }
                break;
            case IDLE:
                cancelItem.setVisible( false );
                statusItem.setVisible( false );
                if( Utils.isNotEmpty( event.text )) {
                    clearItem.setVisible( true );
                    shareItem.setVisible( true );
                    shareProvider.setShareIntent( getShareIntent( event.text ) );
                }
                break;
            case OUTPUT_VIEW_CLEARED:
                clearItem.setVisible( false );
                shareItem.setVisible( false );
                break;
        }
        onPrepareOptionsMenu( menu );
    }

    public boolean isMultiPaneLayout()
    {
        return getResources().getInteger( R.integer.multi_pane_layout ) == 1;
    }

    private Intent getShareIntent( String text )
    {
        Intent intent = new Intent();
        intent.setAction( Intent.ACTION_SEND );
        intent.putExtra( Intent.EXTRA_TEXT, text );
        intent.setType( "text/plain" );
        return intent;
    }

    private boolean[] getActionsVisibility()
    {
        boolean[] result = new boolean[menu.size()];
        for( int i = 0; i < menu.size(); i++ ) {
            result[i] = menu.getItem( i ).isVisible();
        }
        return result;
    }

    private void setActionsVisibility( boolean[] actionsVisibility )
    {
        for( int i = 0; i < menu.size(); i++ ) {
            menu.getItem( i ).setVisible( actionsVisibility[i] );
        }
    }
}
