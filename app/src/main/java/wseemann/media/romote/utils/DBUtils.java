package wseemann.media.romote.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import wseemann.media.romote.database.DeviceDatabase;
import wseemann.media.romote.model.Device;

/**
 * Created by wseemann on 6/20/16.
 */
public class DBUtils {

    private DBUtils() {

    }

    public static int removeDevice(Context context, String serialNumber) {
        int rowsAffected = 0;

        DeviceDatabase deviceDatabase = new DeviceDatabase(context);
        SQLiteDatabase db = deviceDatabase.getWritableDatabase();

        rowsAffected = db.delete(DeviceDatabase.DEVICES_TABLE_NAME, DeviceDatabase.SERIAL_NUMBER + " = ?", new String [] {serialNumber});

        db.close();
        deviceDatabase.close();

        return rowsAffected;
    }

    private static boolean deviceExists(Context context, String serialNumber) {
        boolean exists = false;

        DeviceDatabase deviceDatabase = new DeviceDatabase(context);
        SQLiteDatabase db = deviceDatabase.getWritableDatabase();

        Cursor cursor = db.query(DeviceDatabase.DEVICES_TABLE_NAME, null, DeviceDatabase.SERIAL_NUMBER + " = ?", new String [] {serialNumber}, null, null, null);

        if (cursor.moveToNext()) {
            exists = true;
        }

        cursor.close();

        db.close();
        deviceDatabase.close();

        return exists;
    }

    public static long insertDevice(Context context, Device device) {
        long id = -1;

        if (deviceExists(context, device.getSerialNumber())) {
            return -1;
        }

        DeviceDatabase deviceDatabase = new DeviceDatabase(context);
        SQLiteDatabase db = deviceDatabase.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DeviceDatabase.HOST, device.getHost());
        values.put(DeviceDatabase.UDN, device.getUdn());
        values.put(DeviceDatabase.SERIAL_NUMBER, device.getSerialNumber());
        values.put(DeviceDatabase.DEVICE_ID, device.getDeviceId());
        values.put(DeviceDatabase.VENDOR_NAME, device.getVendorName());
        values.put(DeviceDatabase.MODEL_NUMBER, device.getModelNumber());
        values.put(DeviceDatabase.MODEL_NAME, device.getModelName());
        values.put(DeviceDatabase.WIFI_MAC, device.getWifiMac());
        values.put(DeviceDatabase.ETHERNET_MAC, device.getEthernetMac());
        values.put(DeviceDatabase.NETWORK_TYPE, device.getNetworkType());
        values.put(DeviceDatabase.USER_DEVICE_NAME, device.getUserDeviceName());
        values.put(DeviceDatabase.SOFTWARE_VERSION, device.getSoftwareVersion());
        values.put(DeviceDatabase.SOFTWARE_BUILD, device.getSoftwareBuild());
        values.put(DeviceDatabase.SECURE_DEVICE, device.getSecureDevice());
        values.put(DeviceDatabase.LANGUAGE, device.getLanguage());
        values.put(DeviceDatabase.COUNTY, device.getCountry());
        values.put(DeviceDatabase.LOCALE, device.getLocale());
        values.put(DeviceDatabase.TIME_ZONE, device.getTimeZone());
        values.put(DeviceDatabase.TIME_ZONE_OFFSET, device.getTimeZoneOffset());
        values.put(DeviceDatabase.POWER_MODE, device.getPowerMode());
        values.put(DeviceDatabase.SUPPORTS_SUSPEND, device.getSupportsSuspend());
        values.put(DeviceDatabase.SUPPORTS_FIND_REMOTE, device.getSupportsFindRemote());
        values.put(DeviceDatabase.SUPPORTS_AUDIO_GUIDE, device.getSupportsAudioGuide());
        values.put(DeviceDatabase.DEVELOPER_ENABLED, device.getDeveloperEnabled());
        values.put(DeviceDatabase.KEYED_DEVELOPER_ID, device.getKeyedDeveloperId());
        values.put(DeviceDatabase.SEARCH_ENABLED, device.getSearchEnabled());
        values.put(DeviceDatabase.VOICE_SEARCH_ENABLED, device.getVoiceSearchEnabled());
        values.put(DeviceDatabase.NOTIFICATIONS_ENABLED, device.getNotificationsEnabled());
        values.put(DeviceDatabase.NOTIFICATIONS_FIRST_USE, device.getNotificationsFirstUse());
        values.put(DeviceDatabase.SUPPORTS_PRIVATE_LISTENING, device.getSupportsPrivateListening());
        values.put(DeviceDatabase.HEADPHONES_CONNECTED, device.getHeadphonesConnected());
        values.put(DeviceDatabase.IS_TV, device.getIsTv());
        values.put(DeviceDatabase.IS_STICK, device.getIsStick());
        values.put(DeviceDatabase.CUSTOM_USER_DEVICE_NAME, device.getCustomUserDeviceName());

        id = db.insert(DeviceDatabase.DEVICES_TABLE_NAME, null, values);

        db.close();
        deviceDatabase.close();

        return id;
    }

    public static long updateDevice(Context context, Device device) {
        long id = -1;

        DeviceDatabase deviceDatabase = new DeviceDatabase(context);
        SQLiteDatabase db = deviceDatabase.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DeviceDatabase.HOST, device.getHost());
        values.put(DeviceDatabase.IS_TV, device.getIsTv());
        values.put(DeviceDatabase.IS_STICK, device.getIsStick());
        if (device.getCustomUserDeviceName() != null) {
            values.put(DeviceDatabase.CUSTOM_USER_DEVICE_NAME, device.getCustomUserDeviceName());
        }

        String whereClause = DeviceDatabase.SERIAL_NUMBER + " = ?";
        String [] whereArgs = new String[1];
        whereArgs[0] = device.getSerialNumber();

        id = db.update(DeviceDatabase.DEVICES_TABLE_NAME, values, whereClause, whereArgs);

        db.close();
        deviceDatabase.close();

        return id;
    }

    public static Device getDevice(Context context, String serialNumber) {
        Device device = null;

        if (serialNumber == null) {
            return device;
        }

        DeviceDatabase deviceDatabase = new DeviceDatabase(context);
        SQLiteDatabase db = deviceDatabase.getWritableDatabase();

        Cursor cursor = db.query(DeviceDatabase.DEVICES_TABLE_NAME, null, DeviceDatabase.SERIAL_NUMBER + " = ?", new String [] {serialNumber}, null, null, null);

        if (cursor.moveToNext()) {
            device = parseDevice(cursor);
        }

        cursor.close();

        db.close();
        deviceDatabase.close();

        return device;
    }

    public static List<Device> getAllDevices(Context context) {
        List<Device> devices = new ArrayList<Device>();

        DeviceDatabase deviceDatabase = new DeviceDatabase(context);
        SQLiteDatabase db = deviceDatabase.getWritableDatabase();

        Cursor cursor = db.query(DeviceDatabase.DEVICES_TABLE_NAME, null, null, null, null, null, null);

        while (cursor.moveToNext()) {
            devices.add(parseDevice(cursor));
        }

        cursor.close();

        db.close();
        deviceDatabase.close();

        return devices;
    }

    private static Device parseDevice(Cursor cursor) {
        Device device = new Device();
        device.setHost(cursor.getString(cursor.getColumnIndex(DeviceDatabase.HOST)));
        device.setUdn(cursor.getString(cursor.getColumnIndex(DeviceDatabase.UDN)));
        device.setSerialNumber(cursor.getString(cursor.getColumnIndex(DeviceDatabase.SERIAL_NUMBER)));
        device.setDeviceId(cursor.getString(cursor.getColumnIndex(DeviceDatabase.DEVICE_ID)));
        device.setVendorName(cursor.getString(cursor.getColumnIndex(DeviceDatabase.VENDOR_NAME)));
        device.setModelNumber(cursor.getString(cursor.getColumnIndex(DeviceDatabase.MODEL_NUMBER)));
        device.setModelName(cursor.getString(cursor.getColumnIndex(DeviceDatabase.MODEL_NAME)));
        device.setWifiMac(cursor.getString(cursor.getColumnIndex(DeviceDatabase.WIFI_MAC)));
        device.setEthernetMac(cursor.getString(cursor.getColumnIndex(DeviceDatabase.ETHERNET_MAC)));
        device.setNetworkType(cursor.getString(cursor.getColumnIndex(DeviceDatabase.NETWORK_TYPE)));
        device.setUserDeviceName(cursor.getString(cursor.getColumnIndex(DeviceDatabase.USER_DEVICE_NAME)));
        device.setSoftwareVersion(cursor.getString(cursor.getColumnIndex(DeviceDatabase.SOFTWARE_VERSION)));
        device.setSoftwareBuild(cursor.getString(cursor.getColumnIndex(DeviceDatabase.SOFTWARE_BUILD)));
        device.setSecureDevice(cursor.getString(cursor.getColumnIndex(DeviceDatabase.SECURE_DEVICE)));
        device.setLanguage(cursor.getString(cursor.getColumnIndex(DeviceDatabase.LANGUAGE)));
        device.setCountry(cursor.getString(cursor.getColumnIndex(DeviceDatabase.COUNTY)));
        device.setLocale(cursor.getString(cursor.getColumnIndex(DeviceDatabase.LOCALE)));
        device.setTimeZone(cursor.getString(cursor.getColumnIndex(DeviceDatabase.TIME_ZONE)));
        device.setTimeZoneOffset(cursor.getString(cursor.getColumnIndex(DeviceDatabase.TIME_ZONE_OFFSET)));
        device.setPowerMode(cursor.getString(cursor.getColumnIndex(DeviceDatabase.POWER_MODE)));
        device.setSupportsSuspend(cursor.getString(cursor.getColumnIndex(DeviceDatabase.SUPPORTS_SUSPEND)));
        device.setSupportsFindRemote(cursor.getString(cursor.getColumnIndex(DeviceDatabase.SUPPORTS_FIND_REMOTE)));
        device.setSupportsAudioGuide(cursor.getString(cursor.getColumnIndex(DeviceDatabase.SUPPORTS_AUDIO_GUIDE)));
        device.setDeveloperEnabled(cursor.getString(cursor.getColumnIndex(DeviceDatabase.DEVELOPER_ENABLED)));
        device.setKeyedDeveloperId(cursor.getString(cursor.getColumnIndex(DeviceDatabase.KEYED_DEVELOPER_ID)));
        device.setSearchEnabled(cursor.getString(cursor.getColumnIndex(DeviceDatabase.SEARCH_ENABLED)));
        device.setVoiceSearchEnabled(cursor.getString(cursor.getColumnIndex(DeviceDatabase.VOICE_SEARCH_ENABLED)));
        device.setNotificationsEnabled(cursor.getString(cursor.getColumnIndex(DeviceDatabase.NOTIFICATIONS_ENABLED)));
        device.setNotificationsFirstUse(cursor.getString(cursor.getColumnIndex(DeviceDatabase.NOTIFICATIONS_FIRST_USE)));
        device.setSupportsPrivateListening(cursor.getString(cursor.getColumnIndex(DeviceDatabase.SUPPORTS_PRIVATE_LISTENING)));
        device.setHeadphonesConnected(cursor.getString(cursor.getColumnIndex(DeviceDatabase.HEADPHONES_CONNECTED)));
        device.setIsTv(cursor.getString(cursor.getColumnIndex(DeviceDatabase.IS_TV)));
        device.setIsStick(cursor.getString(cursor.getColumnIndex(DeviceDatabase.IS_STICK)));
        device.setCustomUserDeviceName(cursor.getString(cursor.getColumnIndex(DeviceDatabase.CUSTOM_USER_DEVICE_NAME)));

        return device;
    }
}
