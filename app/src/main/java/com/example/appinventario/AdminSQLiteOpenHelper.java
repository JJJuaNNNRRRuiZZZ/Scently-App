package com.example.appinventario;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

public class AdminSQLiteOpenHelper extends SQLiteOpenHelper {
    public AdminSQLiteOpenHelper (@Nullable Context context
            , @Nullable String name
            , @Nullable SQLiteDatabase.CursorFactory factory
            , int version)
    {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        String query = "CREATE TABLE articulos (codigo INTEGER PRIMARY KEY, descripcion TEXT, precio REAL)";

        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        db.execSQL("DROP TABLE IF EXISTS articulos");
        onCreate(db);
    }
}
