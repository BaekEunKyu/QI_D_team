
package com.example.design;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothService mChatService = null;
    EditText O3_Edit;
    EditText O3AQI_Edit;
    EditText NO2_Edit;
    EditText NO2AQI_Edit;
    EditText CO_Edit;
    EditText COAQI_Edit;
    EditText SO2_Edit;
    EditText SO2AQI_Edit;
    EditText PM_Edit;
    EditText PMAQI_Edit;
    EditText temperature_Edit;
    EditText name_Edit;
    EditText mac_Edit;
    EditText final_Value;
    TextView final_Sensor;
    public String result = "";
    String result_code = "";
    String success_message = "";
    String error_message = "";
    String input_MAC = "";
    String temperature = "";
    String O3 = "";
    String NO2 = "";
    String CO = "";
    String SO2 = "";
    String PM = "";
    String O3_value = "";
    String NO2_value = "";
    String CO_value = "";
    String SO2_value = "";
    String PM_value = "";
    String final_result = "";
    String max_Sensor[] = {"CO_AQI", "PM2_5_AQI", "NO2_AQI", "SO2_AQI", "O3_AQI"};

    public String getInput_MAC() {
        return input_MAC;
    }

    public void setInput_MAC(String input_MAC) {
        this.input_MAC = input_MAC;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_main, parent, false);
        O3_Edit = (EditText) view.findViewById(R.id.S_O3_value);
        O3AQI_Edit = (EditText) view.findViewById(R.id.S_O3_AQI);
        NO2_Edit = (EditText) view.findViewById(R.id.S_NO_value);
        NO2AQI_Edit = (EditText) view.findViewById(R.id.S_NO_AQI);
        CO_Edit = (EditText) view.findViewById(R.id.S_CO_value);
        COAQI_Edit = (EditText) view.findViewById(R.id.S_CO_AQI);
        SO2_Edit = (EditText) view.findViewById(R.id.S_SO_value);
        SO2AQI_Edit = (EditText) view.findViewById(R.id.S_SO_AQI);
        PM_Edit = (EditText) view.findViewById(R.id.S_PM_value);
        PMAQI_Edit = (EditText) view.findViewById(R.id.S_PM_AQI);
        temperature_Edit = (EditText) view.findViewById(R.id.S_temprature_value);
        name_Edit = (EditText)view.findViewById(R.id.S_Bluetooth_name);
        mac_Edit = (EditText)view.findViewById(R.id.S_MAC_address);
        final_Value = (EditText)view.findViewById(R.id.S_final_value);
        final_Sensor = (TextView)view.findViewById(R.id.S_final_sensor);

        SharedPreferences settings = this.getActivity().getSharedPreferences("PREFS", 0);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            name_Edit.setText(mConnectedDeviceName);
                            mac_Edit.setText(getInput_MAC());
                            User_data user_data = (User_data) getActivity().getApplication();
                            user_data.setMACaddress(getInput_MAC());
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    if (readMessage!=null) {
                        float max = 0;
                        float data[] = new float[5];
                        int selected_sensor = 0;
                        try {
                            JSONObject json_data = new JSONObject(readMessage);
                            Log.d("asdf", "receive json: " + json_data.toString());
                            temperature = json_data.optString("temperature");
                            temperature_Edit.setText(temperature + "'C");
                            CO = json_data.optString("CO");
                            CO_Edit.setText(CO + "[ppm]");
                            CO_value = json_data.optString("CO_AQI");
                            COAQI_Edit.setText(CO_value);
                            PM = json_data.optString("PM2_5");
                            PM_Edit.setText(PM + "[ug/m^3]");
                            PM_value = json_data.optString("PM2_5_AQI");
                            PMAQI_Edit.setText(PM_value);
                            NO2 = json_data.optString("NO2");
                            NO2_Edit.setText(NO2 + "[ppb]");
                            NO2_value = json_data.optString("NO2_AQI");
                            NO2AQI_Edit.setText(NO2_value);
                            SO2 = json_data.optString("SO2");
                            SO2_Edit.setText(SO2 + "[ppb]");
                            SO2_value = json_data.optString("SO2_AQI");
                            SO2AQI_Edit.setText(SO2_value);
                            O3 = json_data.optString("O3");
                            O3_Edit.setText(O3 + "[ppb]");
                            O3_value = json_data.optString("O3_AQI");
                            O3AQI_Edit.setText(O3_value);
                            final_result = json_data.optString("final_result");
                            final_Value.setText(final_result);
                            if(Float.valueOf(final_result) >= 0 && Float.valueOf(final_result) < 50)
                                final_Value.setBackgroundColor(getResources().getColor(R.color.AQI_0));
                            else if(Float.valueOf(final_result) >= 50 && Float.valueOf(final_result) < 100)
                                final_Value.setBackgroundColor(getResources().getColor(R.color.AQI_51));
                            else if(Float.valueOf(final_result) >= 100 && Float.valueOf(final_result) < 150)
                                final_Value.setBackgroundColor(getResources().getColor(R.color.AQI_101));
                            else if(Float.valueOf(final_result) >= 150 && Float.valueOf(final_result) < 200)
                                final_Value.setBackgroundColor(getResources().getColor(R.color.AQI_151));
                            else if(Float.valueOf(final_result) >= 200 && Float.valueOf(final_result) < 300)
                                final_Value.setBackgroundColor(getResources().getColor(R.color.AQI_201));
                            else
                                final_Value.setBackgroundColor(getResources().getColor(R.color.AQI_301));
                            data[0] = Float.valueOf(CO_value);
                            data[1] = Float.valueOf(PM_value);
                            data[2] = Float.valueOf(NO2_value);
                            data[3] = Float.valueOf(SO2_value);
                            data[4] = Float.valueOf(O3_value);
                            for(int i = 0; i < data.length; i++) {
                                if (data[i] > max) {
                                    max = data[i];
                                    selected_sensor = i;
                                }
                            }
                            Log.d("asdf", Integer.toString(selected_sensor));
                            final_Sensor.setText(max_Sensor[selected_sensor]);
                        } catch (Exception e) {
                            Log.e("Fail 3", e.toString());
                        }

                        User_data user_data = (User_data)getActivity().getApplication();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String format = simpleDateFormat.format(new Date());
                        Log.d("asdf", "Current Timestamp: " + format);

                        JSONObject json = new JSONObject();
                        try {
                            json.put("SSN", user_data.getSSN());
                            json.put("O3", O3);
                            json.put("NO2", NO2);
                            json.put("CO", CO);
                            json.put("SO2", SO2);
                            json.put("temperature", temperature);
                            json.put("PM2_5", PM);
                            json.put("O3_AQI", O3AQI_Edit.getText().toString());
                            json.put("NO2_AQI", NO2AQI_Edit.getText().toString());
                            json.put("CO_AQI", COAQI_Edit.getText().toString());
                            json.put("SO2_AQI", SO2AQI_Edit.getText().toString());
                            json.put("PM2_5_AQI", PMAQI_Edit.getText().toString());
                            json.put("total_AQI_name", final_Sensor.getText().toString());
                            json.put("total_AQI_value", final_Value.getText().toString());
                            json.put("lat", user_data.getLat());
                            json.put("lng", user_data.getLng());
                            json.put("air_timestamp", format);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        try {
                            result = new PostJSON().execute("http://localhost:8888/data/airquality/transfer", json.toString()).get();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d("asdf", "json: " + json.toString());
                        Log.d("asdf", "result: " + result);

                        try {
                            JSONObject json_data = new JSONObject(result);
                            Toast.makeText(getActivity(), result, Toast.LENGTH_SHORT).show();
                            Log.d("asdf", mac_Edit.getText().toString() + "   receive json: " + json_data.toString());
                            result_code = (json_data.optString("result_code"));
                            success_message = (json_data.optString("success_message"));
                            error_message = (json_data.optString("error_message"));
                            Log.d("asdf", "result_code: " + result_code);
                            Log.d("asdf", "success_message: " + success_message);
                            Log.d("asdf", "error_message: " + error_message);
                        } catch (Exception e) {
                            Log.e("Fail 3", e.toString());
                        }

                        if(result_code.equals("0")){
                            Toast.makeText(getActivity(), success_message, Toast.LENGTH_SHORT).show();
                        }
                        else if(result_code.equals("1")){
                            Toast.makeText(getActivity(), error_message, Toast.LENGTH_SHORT).show();
                        }
                        else
                            Toast.makeText(getActivity(), "no message", Toast.LENGTH_SHORT).show();
                    }

                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        setInput_MAC(address);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bluetooth_button: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }
}