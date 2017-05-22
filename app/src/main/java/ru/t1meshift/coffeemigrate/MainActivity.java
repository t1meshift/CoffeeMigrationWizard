package ru.t1meshift.coffeemigrate;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import eu.chainfire.libsuperuser.Shell;
import org.w3c.dom.Document;
import org.xml.sax.XMLReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;

/*
Как обычно, программирую, используя парадигмы K&V и Stack Overflow programming.
Зато кэш перенесу (наверное).

t1meshift, 21.05.2017, 20:55 VLAT.
 */

public class MainActivity extends AppCompatActivity {
    TextView log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        log = ((TextView) findViewById(R.id.hello_world_id));
        Toast.makeText(this, Environment.getExternalStorageDirectory().getPath(),Toast.LENGTH_LONG).show();
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.KILL_BACKGROUND_PROCESSES}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    try {
                        Process p = Runtime.getRuntime().exec("su"); // тупое говно, надо переписать, чтобы без рута закрывалось к чертям
                        ActivityManager am = (ActivityManager)getApplicationContext().getSystemService(ACTIVITY_SERVICE);
                        am.killBackgroundProcesses("com.vkcoffee.android");
                        am.killBackgroundProcesses("su.operator555.vkcoffee");
                        Shell.SU.run("adb shell killall com.vkcoffee.android"); //чтобы наверняка
                        Shell.SU.run("adb shell killall su.operator555.vkcoffee");
                    }
                    catch (Exception e){}
                    Shell.SU.run("cp -p -f /data/data/com.vkcoffee.android/databases/audio.db " + Environment.getExternalStorageDirectory().getPath() + "/old_vk_audio.db");
                    Shell.SU.run("cp -p -f /data/data/su.operator555.vkcoffee/databases/databaseVerThree.db " + Environment.getExternalStorageDirectory().getPath() + "/new_vk_audio.db");
                    Shell.SU.run("cp -p -f /data/data/com.vkcoffee.android/shared_prefs/GENERAL-1.xml " + Environment.getExternalStorageDirectory().getPath() + "/old_vk_prefs.xml");
                    log.setText(log.getText()+"\nDBs copied.");
                    OldDBHelper oldDBHelper = new OldDBHelper(this);
                    NewDBHelper newDBHelper = new NewDBHelper(this);
                    SQLiteDatabase oldDB = oldDBHelper.getReadableDatabase();
                    SQLiteDatabase newDB = newDBHelper.getWritableDatabase();

                    try {
                        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                        //factory.setNamespaceAware(true); // если используется пространство имён
                        XmlPullParser parser = factory.newPullParser();
                        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/old_vk_prefs.xml");
                        FileInputStream fis = new FileInputStream(file);
                        parser.setInput(new InputStreamReader(fis));

                        boolean useOficPathCache = true;
                        String audioCacheLocation = "", cacheDirCoffeeNew = "";
                        final String oficPathCache = ".vkontakte/cache/audio";

                        String oldCachePath;

                        int event = parser.getEventType();
                        while (event != XmlPullParser.END_DOCUMENT)  {
                            String name=parser.getName();

                            switch (event){
                                case XmlPullParser.START_TAG:
                                    String field = parser.getAttributeValue(null,"name");
                                    //Log.v("CoffeeMig", "Got start tag "+name+" with name "+field);
                                    if (name.equals("boolean"))
                                    {
                                        if (field.equals("useOficPathCache"))
                                            useOficPathCache = parser.getAttributeValue(null,"value").equals("true");
                                    }
                                    else if (name.equals("string"))
                                    {
                                        if (field.equals("audioCacheLocation")) {
                                            parser.next();
                                            audioCacheLocation = parser.getText();

                                        }
                                        else if (field.equals("cacheDirCoffeeNew")) {
                                            parser.next();
                                            cacheDirCoffeeNew = parser.getText();
                                        }
                                    }
                                    break;

                                case XmlPullParser.END_TAG:
                                    //Log.v("CoffeeMig", "Got end tag "+name+" with name "+parser.getAttributeValue(null,"name"));
                                    break;
                            }
                            event = parser.next();
                        }
                        fis.close();
                        Log.v("CoffeeMig", "Using official path: "+String.valueOf(useOficPathCache));
                        Log.v("CoffeeMig", "Cache location: "+audioCacheLocation);
                        Log.v("CoffeeMig", "Cache dir: "+cacheDirCoffeeNew);

                        if (useOficPathCache)
                            oldCachePath = Environment.getExternalStorageDirectory().getPath() + "/" + oficPathCache;
                        else
                            oldCachePath = audioCacheLocation + "/" + cacheDirCoffeeNew;

                        final String[] cols = {"oid", "aid", "title", "artist", "duration", "lyrics_id", "lyrics"};
                        Cursor cursor = oldDB.query("files", cols, null, null, null, null, null);
                        if (cursor.moveToLast()) {
                            ContentValues cv = new ContentValues();
                            int position = 0;
                            int index_oid = cursor.getColumnIndex("oid"),
                                index_aid = cursor.getColumnIndex("aid"),
                                index_title = cursor.getColumnIndex("title"),
                                index_artist = cursor.getColumnIndex("artist"),
                                index_duration = cursor.getColumnIndex("duration"),
                                index_lyrics_id = cursor.getColumnIndex("lyrics_id"),
                                index_lyrics_text = cursor.getColumnIndex("lyrics");

                            do {
                                cv.clear();
                                cv.put("oid", cursor.getInt(index_oid));
                                cv.put("aid", cursor.getInt(index_aid));
                                cv.put("title", cursor.getString(index_title));
                                cv.put("artist", cursor.getString(index_artist));
                                cv.put("duration", cursor.getInt(index_duration));
                                cv.put("lyrics_id", cursor.getInt(index_lyrics_id));
                                cv.put("lyrics_text", cursor.getString(index_lyrics_text));
                                cv.put("position", position++); //saving current order
                                cv.put("url", "https://vk.com/mp3/audio_api_unavailable.mp3"); // dummy mp3 link
                                cv.put("playlist_id", 0);
                                cv.put("restriction", 0);
                                cv.put("file", oldCachePath+"/"+cv.get("oid")+"_"+cv.get("aid"));
                                newDB.insert("saved_track", null, cv);
                            } while (cursor.moveToPrevious());
                        } else
                            Log.d("CoffeeMig","No cache!");

                        cursor.close();

                        oldDB.close();
                        newDB.close();

                        Shell.SU.run("mv /data/data/su.operator555.vkcoffee/databases/databaseVerThree.db /data/data/su.operator555.vkcoffee/databases/databaseVerThree.db_orig");
                        Shell.SU.run("cp -p -f "+Environment.getExternalStorageDirectory().getPath() + "/new_vk_audio.db " + "/data/data/su.operator555.vkcoffee/databases/databaseVerThree.db");
                        //TODO:
                        /*
                        - fix crash on 7.17 app
                          (it could be caused by modified permissions and owner of DB)
                        */
                    } catch (Exception e){
                        e.printStackTrace();
                    }


                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permission denied. Restart the app.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }
}
