package ru.t1meshift.coffeemigrate;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

/**
 * Created by t1meshift on 20.05.2017.
 */
public class OldDBHelper extends SQLiteOpenHelper {

    //public static final String
    public OldDBHelper(Context context) {
        super(context, Environment.getExternalStorageDirectory().getPath()+"/old_vk_audio.db", null, 5);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        Log.e("OLD DB", "Dafuq happened?");
        //database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

    }
}
