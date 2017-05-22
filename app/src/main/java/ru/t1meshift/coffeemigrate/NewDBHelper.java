package ru.t1meshift.coffeemigrate;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

/**
 * Created by yury on 20.05.2017.
 */
//  /data/data/com.vkcoffee.android/databases/audio.db
public class NewDBHelper extends SQLiteOpenHelper {

    //public static final String
    public NewDBHelper(Context context) {
        super(context, Environment.getExternalStorageDirectory().getPath()+"/new_vk_audio.db", null, 22);
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

    @Override
    public void onOpen(SQLiteDatabase db)
    {
        db.delete("saved_track", null, null); //wiping new cache
    }
}
