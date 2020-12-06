package com.inline.mobiletotv;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Vibrator;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Our WalkieTalkie Activity. This Activity has 4 {@link State}s.
 *
 * <p>{@link State#UNKNOWN}: We cannot do anything while we're in this state. The app is likely in
 * the background.
 *
 * <p>{@link State#DISCOVERING}: Our default state (after we've connected). We constantly listen for
 * a device to advertise near us.
 *
 * <p>{@link State#ADVERTISING}: If a user shakes their device, they enter this state. We advertise
 * our device so that others nearby can discover us.
 *
 * <p>{@link State#CONNECTED}: We've connected to another device. We can now talk to them by holding
 * down the volume keys and speaking into the phone. We'll continue to advertise (if we were already
 * advertising) so that more people can connect to us.
 */
public class MainActivity extends BaseConnectionActivity  {
  /** If true, debug logs are shown on the device. */
  private static final boolean DEBUG = true;

  /**
   * The connection strategy we'll use for Nearby Connections. In this case, we've decided on
   * P2P_STAR, which is a combination of Bluetooth Classic and WiFi Hotspots.
   */
  private static final Strategy STRATEGY = Strategy.P2P_STAR;

  /** Acceleration required to detect a shake. In multiples of Earth's gravity. */
  private static final float SHAKE_THRESHOLD_GRAVITY = 2;

  /**
   * Advertise for 30 seconds before going back to discovering. If a client connects, we'll continue
   * to advertise indefinitely so others can still connect.
   */
  private static final long ADVERTISING_DURATION = 30000;

  /** How long to vibrate the phone when we change states. */
  private static final long VIBRATION_STRENGTH = 500;

  /** Length of state change animations. */
  private static final long ANIMATION_DURATION = 600;

  /**
   * This service id lets us find other nearby devices that are interested in the same thing. Our
   * sample does exactly one thing, so we hardcode the ID.
   */
  private static final String SERVICE_ID =
          "com.google.location.nearby.apps.walkietalkie.manual.SERVICE_ID";

  /**
   * The state of the app. As the app changes states, the UI will update and advertising/discovery
   * will start/stop.
   */
  private State mState = State.UNKNOWN;

  /** A random UID used as this device's endpoint name. */
  private String mName;

  /** Displays the previous state during animation transitions. */
  private TextView mPreviousStateView;

  /** Displays the current state. */
  private TextView mCurrentStateView;

  /** An animator that controls the animation from previous state to current state. */
//  @Nullable private Animator mCurrentAnimator;

  /** A running log of debug messages. Only visible when DEBUG=true. */
  private TextView mDebugLogView;

  /** The SensorManager gives us access to sensors on the device. */
//  private SensorManager mSensorManager;

  /** The accelerometer sensor allows us to detect device movement for shake-to-advertise. */
//  private Sensor mAccelerometer;

  /** Listens to holding/releasing the volume rocker. */
//  private final GestureDetector mGestureDetector =
//      new GestureDetector(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP) {
//        @Override
//        protected void onHold() {
//          logV("onHold");
//          startRecording();
//        }
//
//        @Override
//        protected void onRelease() {
//          logV("onRelease");
//          stopRecording();
//        }
//      };

  /** For recording audio as the user speaks. */
//  @Nullable private AudioRecorder mRecorder;

  /** For playing audio from other users nearby. */
//  private final Set<AudioPlayer> mAudioPlayers = new HashSet<>();

  /** The phone's original media volume. */
  private int mOriginalVolume;

  /**
   * A Handler that allows us to post back on to the UI thread. We use this to resume discovery
   * after an uneventful bout of advertising.
   */
  private final Handler mUiHandler = new Handler(Looper.getMainLooper());

  /** Starts discovery. Used in a postDelayed manor with {@link #mUiHandler}. */
  private final Runnable mDiscoverRunnable =
          new Runnable() {
            @Override
            public void run() {
              setState(State.DISCOVERING);
            }
          };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);


    Button btn_send = (Button) findViewById(R.id.btn_send);
    btn_send.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        startSending();

      }
    });

    Button btn_discover = (Button) findViewById(R.id.btn_discover);
    btn_discover.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (getState() == State.DISCOVERING) {
          logD("Device shaken");
          setState(State.ADVERTISING);
          postDelayed(mDiscoverRunnable, ADVERTISING_DURATION);
        }
      }
    });
    mPreviousStateView = (TextView) findViewById(R.id.previous_state);
    mCurrentStateView = (TextView) findViewById(R.id.current_state);

    mDebugLogView = (TextView) findViewById(R.id.debug_log);
    mDebugLogView.setVisibility(DEBUG ? View.VISIBLE : View.GONE);
    mDebugLogView.setMovementMethod(new ScrollingMovementMethod());

    mName = generateRandomName();

    ((TextView) findViewById(R.id.name)).setText(mName);
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if (mState == State.CONNECTED /*&& mGestureDetector.onKeyEvent(event)*/) {
      return true;
    }
    return super.dispatchKeyEvent(event);
  }

  @Override
  protected void onStart() {
    super.onStart();
//    mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);

    // Set the media volume to max.

    setState(State.DISCOVERING);
  }

  @Override
  protected void onStop() {

    setState(State.UNKNOWN);

    mUiHandler.removeCallbacksAndMessages(null);


    super.onStop();
  }

  @Override
  public void onBackPressed() {
    if (getState() == State.CONNECTED || getState() == State.ADVERTISING) {
      setState(State.DISCOVERING);
      return;
    }
    super.onBackPressed();
  }

  @Override
  protected void onEndpointDiscovered(Endpoint endpoint) {
    // We found an advertiser!
    if (!isConnecting()) {
      connectToEndpoint(endpoint);
    }
  }

  @Override
  protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
    // A connection to another device has been initiated! We'll accept the connection immediately.
    acceptConnection(endpoint);
  }

  @Override
  protected void onEndpointConnected(Endpoint endpoint) {
    Toast.makeText(
            this, getString(R.string.toast_connected, endpoint.getName()), Toast.LENGTH_SHORT)
            .show();
    setState(State.CONNECTED);
  }

  @Override
  protected void onEndpointDisconnected(Endpoint endpoint) {
    Toast.makeText(
            this, getString(R.string.toast_disconnected, endpoint.getName()), Toast.LENGTH_SHORT)
            .show();

    // If we lost all our endpoints, then we should reset the state of our app and go back
    // to our initial state (discovering).
    if (getConnectedEndpoints().isEmpty()) {
      setState(State.DISCOVERING);
    }
  }

  @Override
  protected void onConnectionFailed(Endpoint endpoint) {
    // Let's try someone else.
    if (getState() == State.DISCOVERING && !getDiscoveredEndpoints().isEmpty()) {
      connectToEndpoint(pickRandomElem(getDiscoveredEndpoints()));
    }
  }

  /**
   * The state has changed. I wonder what we'll be doing now.
   *
   * @param state The new state.
   */
  private void setState(State state) {
    if (mState == state) {
      logW("State set to " + state + " but already in that state");
      return;
    }

    logD("State set to " + state);
    State oldState = mState;
    mState = state;
    onStateChanged(oldState, state);
  }

  /** @return The current state. */
  private State getState() {
    return mState;
  }

  /**
   * State has changed.
   *
   * @param oldState The previous state we were in. Clean up anything related to this state.
   * @param newState The new state we're now in. Prepare the UI for this state.
   */
  private void onStateChanged(State oldState, State newState) {
//    if (mCurrentAnimator != null && mCurrentAnimator.isRunning()) {
//      mCurrentAnimator.cancel();
//    }

    // Update Nearby Connections to the new state.
    switch (newState) {
      case DISCOVERING:
        if (isAdvertising()) {
          stopAdvertising();
        }
        disconnectFromAllEndpoints();
        startDiscovering();
        break;
      case ADVERTISING:
        if (isDiscovering()) {
          stopDiscovering();
        }
        disconnectFromAllEndpoints();
        startAdvertising();
        break;
      case CONNECTED:
        if (isDiscovering()) {
          stopDiscovering();
        } else if (isAdvertising()) {
          // Continue to advertise, so others can still connect,
          // but clear the discover runnable.
          removeCallbacks(mDiscoverRunnable);
        }
        break;
      case UNKNOWN:
        stopAllEndpoints();
        break;
      default:
        // no-op
        break;
    }

    // Update the UI.
    switch (oldState) {
      case UNKNOWN:
        // Unknown is our initial state. Whatever state we move to,
        // we're transitioning forwards.
        transitionForward(oldState, newState);
        break;
      case DISCOVERING:
        switch (newState) {
          case UNKNOWN:
            transitionBackward(oldState, newState);
            break;
          case ADVERTISING:
          case CONNECTED:
            transitionForward(oldState, newState);
            break;
          default:
            // no-op
            break;
        }
        break;
      case ADVERTISING:
        switch (newState) {
          case UNKNOWN:
          case DISCOVERING:
            transitionBackward(oldState, newState);
            break;
          case CONNECTED:
            transitionForward(oldState, newState);
            break;
          default:
            // no-op
            break;
        }
        break;
      case CONNECTED:
        // Connected is our final state. Whatever new state we move to,
        // we're transitioning backwards.
        transitionBackward(oldState, newState);
        break;
      default:
        // no-op
        break;
    }
  }

  /** Transitions from the old state to the new state with an animation implying moving forward. */
  private void transitionForward(State oldState, final State newState) {
    mPreviousStateView.setVisibility(View.VISIBLE);
    mCurrentStateView.setVisibility(View.VISIBLE);

    updateTextView(mPreviousStateView, oldState);
    updateTextView(mCurrentStateView, newState);

  }

  /** Transitions from the old state to the new state with an animation implying moving backward. */
//  @UiThread
  private void transitionBackward(State oldState, final State newState) {
    mPreviousStateView.setVisibility(View.VISIBLE);
    mCurrentStateView.setVisibility(View.VISIBLE);

    updateTextView(mCurrentStateView, oldState);
    updateTextView(mPreviousStateView, newState);


  }


  /** Updates the {@link TextView} with the correct color/text for the given {@link State}. */
//  @UiThread
  private void updateTextView(TextView textView, State state) {
    switch (state) {
      case DISCOVERING:
        textView.setBackgroundResource(R.color.state_discovering);
        textView.setText(R.string.status_discovering);
        break;
      case ADVERTISING:
        textView.setBackgroundResource(R.color.state_advertising);
        textView.setText(R.string.status_advertising);
        break;
      case CONNECTED:
        textView.setBackgroundResource(R.color.state_connected);
        textView.setText(R.string.status_connected);
        break;
      default:
        textView.setBackgroundResource(R.color.state_unknown);
        textView.setText(R.string.status_unknown);
        break;
    }
  }



  /** {@see ConnectionsActivity#onReceive(Endpoint, Payload)} */
  @Override
  protected void onReceive(Endpoint endpoint, Payload payload) {
    if (payload.getType() == Payload.Type.STREAM) {
      Toast.makeText(this," Received "+new String(payload.asBytes(), UTF_8),
              Toast.LENGTH_LONG).show();

    }else  if (payload.getType() == Payload.Type.BYTES) {
      Toast.makeText(this," Received "+new String(payload.asBytes(), UTF_8),
              Toast.LENGTH_LONG).show();

    }
  }



  /** Starts recording sound from the microphone and streaming it to all connected devices. */
  private void startSending() {
    logV("startSending()");
    try {
      ParcelFileDescriptor[] payloadPipe = ParcelFileDescriptor.createPipe();

      // Send the first half of the payload (the read side) to Nearby Connections.
//      send(Payload.fromStream(payloadPipe[0]));
      send(Payload.fromBytes("Random message".getBytes(UTF_8)));

    } catch (IOException e) {
      logE("startRecording() failed", e);
    }
  }

  /** Stops streaming sound from the microphone. */
  private void stopRecording() {
    logV("stopRecording()");
//    if (mRecorder != null) {
//      mRecorder.stop();
//      mRecorder = null;
//    }
  }

  /** @return True if currently streaming from the microphone. */
//  private boolean isRecording() {
//    return mRecorder != null && mRecorder.isRecording();
//  }

  /** {@see ConnectionsActivity#getRequiredPermissions()} */
  @Override
  protected String[] getRequiredPermissions() {
    return join(
            super.getRequiredPermissions(),
            Manifest.permission.RECORD_AUDIO);
  }

  /** Joins 2 arrays together. */
  private static String[] join(String[] a, String... b) {
    String[] join = new String[a.length + b.length];
    System.arraycopy(a, 0, join, 0, a.length);
    System.arraycopy(b, 0, join, a.length, b.length);
    return join;
  }

  /**
   * Queries the phone's contacts for their own profile, and returns their name. Used when
   * connecting to another device.
   */
  @Override
  protected String getName() {
    return mName;
  }

  /** {@see ConnectionsActivity#getServiceId()} */
  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  /** {@see ConnectionsActivity#getStrategy()} */
  @Override
  public Strategy getStrategy() {
    return STRATEGY;
  }

  /** {@see Handler#post()} */
  protected void post(Runnable r) {
    mUiHandler.post(r);
  }

  /** {@see Handler#postDelayed(Runnable, long)} */
  protected void postDelayed(Runnable r, long duration) {
    mUiHandler.postDelayed(r, duration);
  }

  /** {@see Handler#removeCallbacks(Runnable)} */
  protected void removeCallbacks(Runnable r) {
    mUiHandler.removeCallbacks(r);
  }

  @Override
  protected void logV(String msg) {
    super.logV(msg);
    appendToLogs(toColor(msg, getResources().getColor(R.color.log_verbose)));
  }

  @Override
  protected void logD(String msg) {
    super.logD(msg);
    appendToLogs(toColor(msg, getResources().getColor(R.color.log_debug)));
  }

  @Override
  protected void logW(String msg) {
    super.logW(msg);
    appendToLogs(toColor(msg, getResources().getColor(R.color.log_warning)));
  }

  @Override
  protected void logW(String msg, Throwable e) {
    super.logW(msg, e);
    appendToLogs(toColor(msg, getResources().getColor(R.color.log_warning)));
  }

  @Override
  protected void logE(String msg, Throwable e) {
    super.logE(msg, e);
    appendToLogs(toColor(msg, getResources().getColor(R.color.log_error)));
  }

  private void appendToLogs(CharSequence msg) {
    mDebugLogView.append("\n");
    mDebugLogView.append(DateFormat.format("hh:mm", System.currentTimeMillis()) + ": ");
    mDebugLogView.append(msg);
  }

  private static CharSequence toColor(String msg, int color) {
    SpannableString spannable = new SpannableString(msg);
    spannable.setSpan(new ForegroundColorSpan(color), 0, msg.length(), 0);
    return spannable;
  }

  private static String generateRandomName() {
    String name = "";
    Random random = new Random();
    for (int i = 0; i < 5; i++) {
      name += random.nextInt(10);
    }
    return name;
  }

  @SuppressWarnings("unchecked")
  private static <T> T pickRandomElem(Collection<T> collection) {
    return (T) collection.toArray()[new Random().nextInt(collection.size())];
  }

  /**
   * Provides an implementation of Animator.AnimatorListener so that we only have to override the
   * method(s) we're interested in.
   */
  private abstract static class AnimatorListener implements Animator.AnimatorListener {
    @Override
    public void onAnimationStart(Animator animator) {}

    @Override
    public void onAnimationEnd(Animator animator) {}

    @Override
    public void onAnimationCancel(Animator animator) {}

    @Override
    public void onAnimationRepeat(Animator animator) {}
  }

  /** States that the UI goes through. */
  public enum State {
    UNKNOWN,
    DISCOVERING,
    ADVERTISING,
    CONNECTED
  }
}