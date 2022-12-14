/*
 * Copyright (C) 2015 The Android Open Source Project
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

package org.foldr.fcpp.androidDemo;

import static org.foldr.fcpp.androidDemo.Constants.LOG_TAG;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceFragmentCompat;

/**
 * Setup display fragments and ensure the device supports Bluetooth.
 */
public class MainActivity extends FragmentActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.activity_main_title);
//        mToolbar = findViewById(R.id.AdvertiserToolbar);
//        assert mToolbar != null;

        if (savedInstanceState == null) {
            checkPermissions(this, this);

            mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                    .getAdapter();

            // Is Bluetooth supported on this device?
            if (mBluetoothAdapter != null) {

                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()) {

                    // Are Bluetooth Advertisements supported on this device?
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {

                        if (!mBluetoothAdapter.isLe2MPhySupported()) {
                            showErrorText(R.string.ble_2mphy_failed);
                            return;
                        }
                        if (!mBluetoothAdapter.isLeExtendedAdvertisingSupported()) {
                            showErrorText(R.string.ble_extended_failed);
                            return;
                        } else {
                            Log.i(LOG_TAG, "LE Extended Advertising supported.");
                        }
                        int leMaximumAdvertisingDataLength = mBluetoothAdapter.getLeMaximumAdvertisingDataLength();
                        Log.i(LOG_TAG, "leMaximumAdvertisingDataLength="+leMaximumAdvertisingDataLength);
                        if (leMaximumAdvertisingDataLength <= 31) {
                            // VS not sure if this can actually happen.
                            showErrorText(R.string.ble_advertise_too_small);
                            return;
                        }
                        setupFragments();

                    } else {

                        // Bluetooth Advertisements are not supported.
                        showErrorText(R.string.bt_ads_not_supported);
                    }
                } else {

                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
                }
            } else {

                // Bluetooth is not supported.
                showErrorText(R.string.bt_not_supported);
            }
        }
    }

    static void checkPermissions(Activity activity, Context context){
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_PRIVILEGED,
        };

        if(!hasPermissions(context, PERMISSIONS)){
            ActivityCompat.requestPermissions( activity, PERMISSIONS, PERMISSION_ALL);
        }
    }

    static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onDestroy() {
        AP.fcpp_stop();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_ENABLE_BT:

                if (resultCode == RESULT_OK) {

                    // Bluetooth is now Enabled, are Bluetooth Advertisements supported on
                    // this device?
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {

                        // Everything is supported and enabled, load the fragments.
                        setupFragments();

                    } else {

                        // Bluetooth Advertisements are not supported.
                        showErrorText(R.string.bt_ads_not_supported);
                    }
                } else {

                    // User declined to enable Bluetooth, exit the app.
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }

            case Constants.REQUEST_AFL:

                if (resultCode != RESULT_OK) {
                    showErrorText(R.string.afl_failed);
                }

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static class BLEPreferencesFragments extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }
    }

    private void setupFragments() {

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        ScannerFragment scannerFragment = new ScannerFragment();
        // Fragments can't access system services directly, so pass it the BluetoothAdapter
        scannerFragment.setBluetoothAdapter(mBluetoothAdapter);
        transaction.replace(R.id.scanner_fragment_container, scannerFragment);

        AdvertiserFragment advertiserFragment = new AdvertiserFragment();
        transaction.replace(R.id.advertiser_fragment_container, advertiserFragment);

        // TODO: hook up to menu or something instead.
        PreferenceFragmentCompat preferencesFragment = new BLEPreferencesFragments();
        transaction.replace(R.id.preferences_fragment_container, preferencesFragment);

        transaction.commit();
    }

    private void showErrorText(int messageId) {
        Log.e(LOG_TAG, getString(messageId));
        TextView view = (TextView) findViewById(R.id.error_textview);
        view.setText(getString(messageId));
    }
}