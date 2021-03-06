package edu.umich.si.inteco.tutorial2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import edu.umich.si.inteco.minuku.config.Constants;
import edu.umich.si.inteco.minuku.config.UserPreferences;
import edu.umich.si.inteco.minuku.dao.LocationDataRecordDAO;
import edu.umich.si.inteco.minuku.manager.MinukuDAOManager;
import edu.umich.si.inteco.minuku.manager.MinukuNotificationManager;
import edu.umich.si.inteco.minuku.manager.MinukuStreamManager;
import edu.umich.si.inteco.minuku.model.LocationDataRecord;
import edu.umich.si.inteco.minuku.streamgenerator.LocationStreamGenerator;
import edu.umich.si.inteco.minukucore.exception.StreamNotFoundException;

public class MainActivity extends AppCompatActivity {

    TextView latitude = null;
    TextView longitude = null;
    LocationDataRecord currentLocation = null;
    private static final int READ_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitude = (TextView) findViewById(R.id.current_latitude);
        longitude = (TextView) findViewById(R.id.current_longitude);

        Constants.getInstance().setFirebaseUrl(getResources().getString(R.string.UNIQUE_FIREBASE_ROOT_URL));
        Constants.getInstance().setAppName(getResources().getString(R.string.app_name));

        setUserID();
        startService(new Intent(getBaseContext(), BackgroundService.class));
        startService(new Intent(getBaseContext(), MinukuNotificationManager.class));

        initialize();
    }


    private void setUserID() {
        if (UserPreferences.getInstance().getPreference(Constants.ID_SHAREDPREF_EMAIL) == null) {
            String email = "test@example.com"; // would be replaced by the user's email
            UserPreferences.getInstance().writePreference(Constants.ID_SHAREDPREF_EMAIL, email);
            UserPreferences.getInstance().writePreference(Constants.KEY_ENCODED_EMAIL,
                    encodeEmail(email));
        }
    }

    public static final String encodeEmail(String unencodedEmail) {
        if (unencodedEmail == null) return null;
        return unencodedEmail.replace(".", ",");
    }

    private void initialize() {
        MinukuDAOManager daoManager = MinukuDAOManager.getInstance();
        MinukuNotificationManager notificationManager = new MinukuNotificationManager();

        LocationDataRecordDAO locationDAO = new LocationDataRecordDAO();
        daoManager.registerDaoFor(LocationDataRecord.class, locationDAO);

        LocationStreamGenerator locationStreamGenerator =
                new LocationStreamGenerator(getApplicationContext());

        LocationChangeSituation situation = new LocationChangeSituation();
        LocationChangeAction action = new LocationChangeAction();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    READ_LOCATION);
        } else {
            refreshLocation();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case READ_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    refreshLocation();
                } else {
                    finish();
                }
                return;
            }
        }
    }

    private void refreshLocation(){

        try {
            currentLocation =
                    MinukuStreamManager.getInstance().getStreamFor(LocationDataRecord.class).getCurrentValue();
        } catch (StreamNotFoundException e) {
            e.printStackTrace();
        }

        if(currentLocation!= null) {
            latitude.setText(String.valueOf(currentLocation.getLatitude()));
            longitude.setText(String.valueOf(currentLocation.getLongitude()));
        }
        else {
            latitude.setText("unknown");
            longitude.setText("unknown");
        }
    }
}
