package com.compscieddy.meetinthemiddle;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.compscieddy.meetinthemiddle.util.Lawg;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by ambar on 6/17/16.
 */
public class ActivityRecognitionService extends IntentService {

  private static final Lawg L = Lawg.newInstance(ActivityRecognition.class.getSimpleName());
  private File mFile;
  final String mFilename = "activity_log.txt";

  public ActivityRecognitionService() {
    this("ActivityRecognitionService");
  }

  public ActivityRecognitionService(String name) {
    super(name);
  }

  private void appendToFile(String line) {
    FileOutputStream outputStream;
    try {
      outputStream = openFileOutput(mFilename, Context.MODE_APPEND);
      outputStream.write(line.getBytes());
      outputStream.close();
    } catch (Exception e) {
      L.e("Error while trying to write a line to a text file in ActivityRecognitionService:writeToFile()");
      e.printStackTrace();
    }
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    init();
    if (ActivityRecognitionResult.hasResult(intent)) {
      ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
      handleDetectedActivities(result.getProbableActivities());
    }
  }

  private void init() {
    L.d("init " + getApplicationContext().getExternalFilesDir(null) + " filename: " + mFilename);
    mFile = new File(getApplicationContext().getExternalFilesDir(null), mFilename);
  }

  private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
    Date date = new Date();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
    String timestampLogTitle = dateFormat.format(date);

    for( DetectedActivity activity : probableActivities ) {
      int activityType = activity.getType();
      switch(activityType) {
        case DetectedActivity.IN_VEHICLE: {
          L.d( "ActivityRecogition - In Vehicle: " + activity.getConfidence() );
          appendToFile(timestampLogTitle + "Vehicle " + activity.getConfidence());
          break;
        }
        case DetectedActivity.ON_BICYCLE: {
          L.d( "ActivityRecogition - On Bicycle: " + activity.getConfidence() );
          appendToFile(timestampLogTitle + "Bicycle " + activity.getConfidence());
          break;
        }
        case DetectedActivity.ON_FOOT: {
          L.d( "ActivityRecogition - On Foot: " + activity.getConfidence() );
          appendToFile(timestampLogTitle + "On Foot " + activity.getConfidence());
          break;
        }
        case DetectedActivity.RUNNING: {
          L.d( "ActivityRecogition - Running: " + activity.getConfidence() );
          appendToFile(timestampLogTitle + "Running " + activity.getConfidence());
          break;
        }
        case DetectedActivity.STILL: {
          L.d( "ActivityRecogition - Still: " + activity.getConfidence() );
          appendToFile(timestampLogTitle + "Still " + activity.getConfidence());
          break;
        }
        case DetectedActivity.TILTING: {
          L.d( "ActivityRecogition - Tilting: " + activity.getConfidence() );
          appendToFile(timestampLogTitle + "Tilting " + activity.getConfidence());
          break;
        }
        case DetectedActivity.WALKING: {
          L.d( "ActivityRecogition - Walking: " + activity.getConfidence() );
          appendToFile(timestampLogTitle + "Walking " + activity.getConfidence());
          break;
        }
        case DetectedActivity.UNKNOWN: {
          L.d( "ActivityRecogition - Unknown: " + activity.getConfidence() );
          appendToFile(timestampLogTitle + "Unknown " + activity.getConfidence());
          break;
        }
      }
    }
  }
}
