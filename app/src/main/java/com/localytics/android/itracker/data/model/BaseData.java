package com.localytics.android.itracker.data.model;

import android.database.Cursor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by bbo on 3/2/16.
 */
public class BaseData {
    public long time;

    public static BaseData objectForCursor(Class<?> cls, Cursor cursor) {
        try {
            Constructor<BaseData> constructor = (Constructor<BaseData>) cls.getConstructor(Cursor.class);
            return constructor.newInstance(new Object[]{ cursor });
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
