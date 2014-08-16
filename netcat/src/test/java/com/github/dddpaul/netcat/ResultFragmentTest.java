package com.github.dddpaul.netcat;

import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.github.dddpaul.netcat.ui.ResultFragment;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowToast;

import static com.github.dddpaul.netcat.NetCater.Op.CONNECT;
import static com.github.dddpaul.netcat.NetCater.Op.LISTEN;
import static com.github.dddpaul.netcat.NetCater.Proto.*;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.robolectric.util.FragmentTestUtil.startFragment;

@Config( emulateSdk = 18 )
@RunWith( RobolectricTestRunner.class )
public class ResultFragmentTest
{
    final String CLASS_NAME = ( (Object) this ).getClass().getSimpleName();

    private ResultFragment fragment;

    @Mock
    private NetCater mockNetCat;

    @BeforeClass
    public static void init()
    {
        ShadowLog.stream = System.out;
    }

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks( this );
        fragment = new ResultFragment();
    }

    /**
     * Test send button behaviour
     */
    @Test
    public void testSendButton()
    {
        startFragment( fragment );
        when( mockNetCat.isConnected() ).thenReturn( true );
        fragment.setNetCat( mockNetCat );

        Button sendButton = (Button) fragment.getView().findViewById( R.id.b_send );

        assertThat( sendButton ).isDisabled();

        EditText inputText = (EditText) fragment.getView().findViewById( R.id.et_input );
        inputText.setText( "some text" );

        assertThat( sendButton ).isEnabled();
    }

    /**
     * Test connect button handling (from {@link com.github.dddpaul.netcat.ui.MainFragment})
     */
    @Test
    public void testConnect()
    {
        startFragment( fragment );

        // When netCat is connected error should be toasted
        when( mockNetCat.isConnected() ).thenReturn( true );
        fragment.setNetCat( mockNetCat );
        fragment.connect( "" );

        assertThat( ShadowToast.getTextOfLatestToast(), is( fragment.getString( R.string.error_disconnect_first ) ) );

        // When netCat is listening error should be toasted
        when( mockNetCat.isConnected() ).thenReturn( false );
        when( mockNetCat.isListening() ).thenReturn( true );
        fragment.setNetCat( mockNetCat );
        fragment.connect( "" );

        assertThat( ShadowToast.getTextOfLatestToast(), is( fragment.getString( R.string.error_disconnect_first ) ) );

        // When netCat is not connected nor listening but connectTo string has invalid format error should be toasted too
        when( mockNetCat.isConnected() ).thenReturn( false );
        when( mockNetCat.isListening() ).thenReturn( false );
        fragment.setNetCat( mockNetCat );
        fragment.connect( "some improper connectTo string" );

        assertThat( ShadowToast.getTextOfLatestToast(), is( fragment.getString( R.string.error_host_port_format ) ) );

        // Test proper connect at last
        fragment.connect( "TCP:127.0.0.1:9999" );

        verify( mockNetCat ).execute( CONNECT.toString(), TCP.toString(), "127.0.0.1", "9999" );
    }

    /**
     * Test listen button handling (from {@link com.github.dddpaul.netcat.ui.MainFragment})
     */
    @Test
    public void testListen()
    {
        startFragment( fragment );

        // When netCat is listening error should be toasted
        when( mockNetCat.isListening() ).thenReturn( true );
        fragment.setNetCat( mockNetCat );
        fragment.connect( "" );

        assertThat( ShadowToast.getTextOfLatestToast(), is( fragment.getString( R.string.error_disconnect_first ) ) );

        // When netCat is not listening but connectTo string has invalid format error should be toasted too
        when( mockNetCat.isListening() ).thenReturn( false );
        fragment.setNetCat( mockNetCat );
        fragment.listen( "some improper port string" );

        assertThat( ShadowToast.getTextOfLatestToast(), is( fragment.getString( R.string.error_port_format ) ) );

        // Test proper listen at last
        fragment.listen( "UDP:9999" );

        verify( mockNetCat ).execute( LISTEN.toString(), UDP.toString(), "9999" );
    }
}
