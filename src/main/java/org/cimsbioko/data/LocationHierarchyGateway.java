package org.cimsbioko.data;

import android.content.ContentValues;
import android.database.Cursor;
import org.cimsbioko.App;
import org.cimsbioko.model.core.LocationHierarchy;
import org.jetbrains.annotations.NotNull;

import static org.cimsbioko.App.HierarchyItems.*;
import static org.cimsbioko.data.CursorConvert.extractString;

/**
 * Convert LocationHierarchy items to and from database.  LocationHierarchy-specific queries.
 */
public class LocationHierarchyGateway extends Gateway<LocationHierarchy> {

    private static final LocationHierarchyEntityConverter ENTITY_CONVERTER = new LocationHierarchyEntityConverter();
    private static final LocationHierarchyWrapperConverter WRAPPER_CONVERTER = new LocationHierarchyWrapperConverter();
    private static final LocationHierarchyContentValuesConverter CONTENT_VALUES_CONVERTER = new LocationHierarchyContentValuesConverter();

    LocationHierarchyGateway() {
        super(App.HierarchyItems.CONTENT_ID_URI_BASE, COLUMN_HIERARCHY_UUID);
    }

    public Query<LocationHierarchy> findByLevel(String level) {
        return new Query<>(this, getTableUri(), COLUMN_HIERARCHY_LEVEL, level, COLUMN_HIERARCHY_UUID);
    }

    public Query<LocationHierarchy> findByParent(String parentId) {
        return new Query<>(this, getTableUri(), COLUMN_HIERARCHY_PARENT, parentId, COLUMN_HIERARCHY_EXTID);
    }

    @Override
    public String getId(LocationHierarchy entity) {
        return entity.getUuid();
    }

    @Override
    public CursorConverter<LocationHierarchy> getEntityConverter() {
        return ENTITY_CONVERTER;
    }

    @Override
    public CursorConverter<DataWrapper> getWrapperConverter() {
        return WRAPPER_CONVERTER;
    }

    @Override
    public ContentValuesConverter<LocationHierarchy> getContentValuesConverter() {
        return CONTENT_VALUES_CONVERTER;
    }
}

class LocationHierarchyEntityConverter implements CursorConverter<LocationHierarchy> {

    @Override
    @NotNull
    public LocationHierarchy convert(@NotNull Cursor c) {
        LocationHierarchy locationHierarchy = new LocationHierarchy();
        locationHierarchy.setUuid(extractString(c, COLUMN_HIERARCHY_UUID));
        locationHierarchy.setExtId(extractString(c, COLUMN_HIERARCHY_EXTID));
        locationHierarchy.setName(extractString(c, COLUMN_HIERARCHY_NAME));
        locationHierarchy.setLevel(extractString(c, COLUMN_HIERARCHY_LEVEL));
        locationHierarchy.setParentUuid(extractString(c, COLUMN_HIERARCHY_PARENT));
        locationHierarchy.setAttrs(extractString(c, COLUMN_HIERARCHY_ATTRS));
        return locationHierarchy;
    }
}

class LocationHierarchyWrapperConverter implements CursorConverter<DataWrapper> {
    @Override
    @NotNull
    public DataWrapper convert(@NotNull Cursor c) {
        return new DataWrapper(
                extractString(c, COLUMN_HIERARCHY_UUID),
                extractString(c, COLUMN_HIERARCHY_LEVEL),
                extractString(c, COLUMN_HIERARCHY_EXTID),
                extractString(c, COLUMN_HIERARCHY_NAME)
        );
    }
}

class LocationHierarchyContentValuesConverter implements ContentValuesConverter<LocationHierarchy> {

    @Override
    @NotNull
    public ContentValues toContentValues(@NotNull LocationHierarchy locationHierarchy) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_HIERARCHY_UUID, locationHierarchy.getUuid());
        contentValues.put(COLUMN_HIERARCHY_EXTID, locationHierarchy.getExtId());
        contentValues.put(COLUMN_HIERARCHY_NAME, locationHierarchy.getName());
        contentValues.put(COLUMN_HIERARCHY_LEVEL, locationHierarchy.getLevel());
        contentValues.put(COLUMN_HIERARCHY_PARENT, locationHierarchy.getParentUuid());
        contentValues.put(COLUMN_HIERARCHY_ATTRS, locationHierarchy.getAttrs());
        return contentValues;
    }
}
