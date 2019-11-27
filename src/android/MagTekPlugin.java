package com.salonsuitesolutions.magtek;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import com.magtek.mobile.android.mtlib.IMTCardData;
import com.magtek.mobile.android.mtlib.MTConnectionState;
import com.magtek.mobile.android.mtlib.MTConnectionType;
import com.magtek.mobile.android.mtlib.MTSCRA;
import com.magtek.mobile.android.mtlib.MTSCRAEvent;

public class MagTekPlugin extends CordovaPlugin {

    private boolean pluginStarted = false;

    // actions
    private static final String CHECK_PERMISSIONS = "checkPermissions";

    private static final String REGISTER_CARD_SWIPE_CALLBACK = "registerCardSwipeCallback";
    private static final String REGISTER_CONNECTED_CALLBACK = "registerConnectedCallback";
    private static final String REGISTER_DISCONNECTED_CALLBACK = "registerDisconnectedCallback";
    private static final String REGISTER_ERROR_CALLBACK = "registerErrorCallback";

    private static final String STOP = "stop";
    private static final String START = "start";

    // callbacks
    private CallbackContext connectedCallback;
    private CallbackContext disconnectedCallback;
    private CallbackContext errorCallback;
    private CallbackContext cardSwipeDetectedCallback;

    private static final String TAG = "MagTekPlugin";

    // Android 23 requires user to explicitly grant permission for audio
    private static final String RECORD_AUDIO = Manifest.permission.RECORD_AUDIO;
    private static final int CHECK_PERMISSIONS_REQ_CODE = 2;
    private CallbackContext permissionCallback;

    private MTConnectionType connectionType = MTConnectionType.Audio;
    private AudioManager audioManager;
    private int originalVolume;

    private MTSCRA magtek;

    private final HeadsetBroadCastReceiver headsetBroadCastReceiver = new HeadsetBroadCastReceiver();
    private final NoisyAudioStreamReceiver noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();

    private Handler magtekHandler = new Handler(new MagTekHandlerCallback());

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        LOG.i(TAG, "Cordova MagTek Plugin");
        LOG.i(TAG, "(c)2017 Salon Suite Solutions");

        if (cordova.hasPermission(RECORD_AUDIO)) {
            initAudio();
        }

    }

    @Override
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {

        LOG.d(TAG, "action = " + action);

        boolean validAction = true;

        if (action.equals(CHECK_PERMISSIONS)) {

            if (cordova.hasPermission(RECORD_AUDIO)) {
                initAudio();
                callbackContext.success();
            } else {
                permissionCallback = callbackContext;
                cordova.requestPermission(this, CHECK_PERMISSIONS_REQ_CODE, RECORD_AUDIO);
            }

        } else if (action.equals(REGISTER_CARD_SWIPE_CALLBACK)) {

            this.cardSwipeDetectedCallback = callbackContext;
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);

        } else if (action.equals(REGISTER_CONNECTED_CALLBACK)) {

            this.connectedCallback = callbackContext;
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);

        } else if (action.equals(REGISTER_DISCONNECTED_CALLBACK)) {

            this.disconnectedCallback = callbackContext;
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);

        } else if (action.equals(REGISTER_ERROR_CALLBACK)) {

            this.errorCallback = callbackContext;
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);

        } else if (action.equals(START)) {

            pluginStarted = true;
            registerReceivers();
            callbackContext.success();

        } else if (action.equals(STOP)) {

            pluginStarted = false;
            closeDevice(); // receivers send disconnect
            unregisterReceivers();
            callbackContext.success();

        } else {

            validAction = false;

        }

        return validAction;
    }

    private void initAudio() {

        if (audioManager == null) {
            audioManager = (AudioManager) this.cordova.getActivity().getSystemService(Context.AUDIO_SERVICE);
        }

    }

    private void registerReceivers() {
        if (pluginStarted) {
            Activity activity = this.cordova.getActivity();

            activity.registerReceiver(headsetBroadCastReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
            activity.registerReceiver(noisyAudioStreamReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        }
    }

    private void unregisterReceivers() {

        Activity activity = this.cordova.getActivity();

        activity.unregisterReceiver(headsetBroadCastReceiver);
        activity.unregisterReceiver(noisyAudioStreamReceiver);
    }

    @Override
    public void onDestroy() {
        magtek.closeDevice();
        super.onDestroy();
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);

        Log.i(TAG, "onPause");

        if (magtek != null && magtek.isDeviceConnected()) {
            if (connectionType == MTConnectionType.Audio) {
                magtek.closeDevice();
            }
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        Log.i(TAG, "onResume");
        registerReceivers();
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) /* throws JSONException */ {
        for(int result:grantResults) {
            if(result == PackageManager.PERMISSION_DENIED) {
                LOG.d(TAG, "User *rejected* Permissions");
                this.permissionCallback.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Record audio permission denied"));
                return;
            }
        }

        switch(requestCode) {
            case CHECK_PERMISSIONS_REQ_CODE:
                LOG.d(TAG, "User granted Record Audio Access");
                initAudio();
                permissionCallback.success();
                break;
        }
    }

    // MagTek
    private class MagTekHandlerCallback implements Handler.Callback {
        public boolean handleMessage(Message msg) {
            try {
                Log.i(TAG, "Callback " + msg.what);
                switch (msg.what) {
                    case MTSCRAEvent.OnDeviceConnectionStateChanged:
                        onDeviceStateChanged((MTConnectionState) msg.obj);
                        break;
                    case MTSCRAEvent.OnDataReceived:
                        onCardDataReceived((IMTCardData) msg.obj);
                        break;
                    default:
                        LOG.d(TAG, "Ignoring MTSCRAEvent " + msg.what);
                }
            } catch (Exception ex) {
                Log.e(TAG, ex.getLocalizedMessage(), ex);
            }

            return true;
        }
    }

    private void onDeviceStateChanged(MTConnectionState deviceState) {
        switch (deviceState) {
            case Disconnected:
                Log.i(TAG, "OnDeviceStateChanged=Disconnected");
                restoreVolume();
                sendDisconnected();
                break;
            case Connected:
                Log.i(TAG, "OnDeviceStateChanged=Connected");
                setVolumeToMax();
                sendConnected();
                break;
            case Error:
                Log.i(TAG, "OnDeviceStateChanged=Error");
                break;
            case Connecting:
                Log.i(TAG, "OnDeviceStateChanged=Connecting");
                break;
            case Disconnecting:
                Log.i(TAG, "OnDeviceStateChanged=Disconnecting");
                break;
        }
    }

    private void sendConnected() {
        PluginResult result = new PluginResult(PluginResult.Status.OK);
        result.setKeepCallback(true);
        connectedCallback.sendPluginResult(result);
    }

    private void sendDisconnected() {
        PluginResult result = new PluginResult(PluginResult.Status.OK);
        result.setKeepCallback(true);
        disconnectedCallback.sendPluginResult(result);
    }

    private void sendError(String message) {
        // Sending success callback since the error callback is expecting errors
        PluginResult result = new PluginResult(PluginResult.Status.OK, message);
        result.setKeepCallback(true);
        errorCallback.sendPluginResult(result);
    }

    private void onCardDataReceived(IMTCardData cardData) {
        JSONObject cardInfo;
        try {
            cardInfo = asJSONObject(cardData);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
            cardInfo = new JSONObject();
        }
        PluginResult result = new PluginResult(PluginResult.Status.OK, cardInfo);
        result.setKeepCallback(true);
        cardSwipeDetectedCallback.sendPluginResult(result);
    }


    private JSONObject asJSONObject(IMTCardData cardData) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("Tracks.Masked", cardData.getMaskedTracks());
        json.put("Track1.Encrypted", cardData.getTrack1());
        json.put("Track2.Encrypted", cardData.getTrack2());
        json.put("Track3.Encrypted", cardData.getTrack3());
        json.put("Track1.Masked", cardData.getTrack1Masked());
        json.put("Track2.Masked", cardData.getTrack2Masked());
        json.put("Track3.Masked", cardData.getTrack3Masked());

        json.put("MagnePrint.Encrypted", cardData.getMagnePrint());
        json.put("MagnePrint.Status", cardData.getMagnePrintStatus());
        json.put("Device.Serial", cardData.getDeviceSerial());
        json.put("Session.ID", cardData.getSessionID());
        json.put("KSN", cardData.getKSN());

        json.put("Device.Name", magtek.getDeviceName());
        json.put("Swipe.Count", magtek.getSwipeCount());

        json.put("Cap.MagnePrint", cardData.getCapMagnePrint());
        json.put("Cap.MagnePrintEncryption", cardData.getCapMagnePrintEncryption());
        json.put("Cap.MagneSafe20Encryption", cardData.getCapMagneSafe20Encryption());
        json.put("Cap.MagStripeEncryption", cardData.getCapMagStripeEncryption());
        json.put("Cap.MSR", cardData.getCapMSR());
        json.put("Cap.Tracks", cardData.getCapTracks());

        json.put("Card.Data.CRC", cardData.getCardDataCRC());
        json.put("Card.Exp.Date", cardData.getCardExpDate());
        json.put("Card.IIN", cardData.getCardIIN());
        json.put("Card.Last4", cardData.getCardLast4());
        json.put("Card.Name", cardData.getCardName());
        json.put("Card.PAN", cardData.getCardPAN());
        json.put("Card.PAN.Length", cardData.getCardPANLength());
        json.put("Card.Service.Code", cardData.getCardServiceCode());
        json.put("Card.Status", cardData.getCardStatus());

        json.put("HashCode", cardData.getHashCode());
        json.put("Data.Field.Count", cardData.getDataFieldCount());

        json.put("Encryption.Status", cardData.getEncryptionStatus());

        json.put("Firmware", magtek.getFirmware());
        //stringBuilder.append(formatStringIfNotEmpty("Firmware=%s \n", magtek.getFirmware()));

        json.put("MagTek.Device.Serial", cardData.getMagTekDeviceSerial());

        json.put("Response.Type", cardData.getResponseType());
        json.put("TLV.Version", cardData.getTLVVersion());

        json.put("Track.Decode.Status", cardData.getTrackDecodeStatus());

        String tkStatus = cardData.getTrackDecodeStatus();

        if (tkStatus.length() >= 6) {
            String tk1Status = tkStatus.substring(0, 2);
            String tk2Status = tkStatus.substring(2, 4);
            String tk3Status = tkStatus.substring(4, 6);

            json.put("Track1.Status", tk1Status);
            json.put("Track2.Status", tk2Status);
            json.put("Track3.Status", tk3Status);
        }

        json.put("SDK.Version", magtek.getSDKVersion());

        json.put("Battery.Level", magtek.getBatteryLevel());

        //json.put("Raw Data", magtek.getResponseData());
        json.put("TLV Payload", cardData.getTLVPayload());

        return json;
    }

    public class NoisyAudioStreamReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // If the device is unplugged, this immediately detects and closes the device
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (connectionType == MTConnectionType.Audio) {
                    closeDevice();
                    restoreVolume();
                    sendDisconnected();
                }
            }
        }
    }

    public class HeadsetBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            try {
                String action = intent.getAction();

                if ((action.compareTo(Intent.ACTION_HEADSET_PLUG)) == 0) {

                    int headsetState = intent.getIntExtra("state", 0);
                    int hasMicrophone = intent.getIntExtra("microphone", 0);

                    if ((headsetState == 1) && (hasMicrophone == 1))  {  // headset was plugged in & has a microphone
                        Log.i(TAG, "MagTek (or headset with microphone) was connected");
                        openDevice();
                    }

                }

            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
            }
        }
    }

    private void openDevice() {
        Log.i(TAG, "openDevice");

        if (magtek == null) {
            magtek = new MTSCRA(this.cordova.getActivity(), magtekHandler);
            magtek.setConnectionType(MTConnectionType.Audio);
            Log.i(TAG, "Created new magtek device");
        }

        if (!magtek.isDeviceConnected()) {
            magtek.openDevice();
        }

    }

    private void closeDevice() {
        Log.i(TAG, "closeDevice");
        if (magtek != null) {
            magtek.closeDevice();
        }
    }

    private void setVolume(int volume) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
    }

    private void saveVolume() {
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void restoreVolume() {
        setVolume(originalVolume);
    }

    private void setVolumeToMax() {
        saveVolume();
        int volume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        setVolume(volume);
    }

}
