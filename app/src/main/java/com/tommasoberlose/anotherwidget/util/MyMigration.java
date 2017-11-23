package com.tommasoberlose.anotherwidget.util;

import android.support.annotation.NonNull;
import android.util.Log;

import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;

/**
 * Created by tommaso on 06/11/17.
 */

public class MyMigration implements RealmMigration {
    @Override
    public void migrate(@NonNull DynamicRealm realm, long oldVersion, long newVersion) {

        RealmSchema schema = realm.getSchema();
        if (oldVersion == 1) {
            RealmObjectSchema event = schema.get("Event");
            if (event != null) {
                if (!event.hasField("eventID")) {
                    event.addField("eventID", long.class);
                }
                event
                        .addField("id_tmp", long.class)
                        .transform(new RealmObjectSchema.Function() {
                            @Override
                            public void apply(@NonNull DynamicRealmObject obj) {
                                obj.setLong("id_tmp", obj.getInt("id"));
                            }
                        })
                        .removeField("id")
                        .renameField("id_tmp", "id");
            }
            oldVersion++;
        }
    }

    @Override
    public int hashCode() {
        return 37;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof MyMigration);
    }
}