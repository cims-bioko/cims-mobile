package org.openhds.mobile.repository.gateway;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;

import org.openhds.mobile.OpenHDS;
import org.openhds.mobile.R;
import org.openhds.mobile.model.core.Individual;
import org.openhds.mobile.repository.Converter;
import org.openhds.mobile.repository.DataWrapper;
import org.openhds.mobile.repository.Query;

import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_ATTRS;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_DOB;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_EXTID;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_FIRST_NAME;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_GENDER;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_LANGUAGE_PREFERENCE;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_LAST_NAME;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_NATIONALITY;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_OTHER_ID;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_OTHER_NAMES;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_OTHER_PHONE_NUMBER;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_PHONE_NUMBER;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_POINT_OF_CONTACT_NAME;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_POINT_OF_CONTACT_PHONE_NUMBER;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_RELATIONSHIP_TO_HEAD;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_RESIDENCE_LOCATION_UUID;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_STATUS;
import static org.openhds.mobile.OpenHDS.Individuals.COLUMN_INDIVIDUAL_UUID;
import static org.openhds.mobile.model.core.Individual.getFullName;
import static org.openhds.mobile.repository.RepositoryUtils.extractString;


/**
 * Convert Individuals to and from database.  Individual-specific queries.
 */
public class IndividualGateway extends Gateway<Individual> {

    public IndividualGateway() {
        super(OpenHDS.Individuals.CONTENT_ID_URI_BASE, COLUMN_INDIVIDUAL_UUID, new IndividualConverter());
    }

    public Query findByResidency(String residencyId) {
        return new Query(
                tableUri, COLUMN_INDIVIDUAL_RESIDENCE_LOCATION_UUID, residencyId, COLUMN_INDIVIDUAL_EXTID);
    }

    private static class IndividualConverter implements Converter<Individual> {

        @Override
        public Individual fromCursor(Cursor cursor) {
            Individual individual = new Individual();
            individual.setUuid(extractString(cursor, COLUMN_INDIVIDUAL_UUID));
            individual.setExtId(extractString(cursor, COLUMN_INDIVIDUAL_EXTID));
            individual.setFirstName(extractString(cursor, COLUMN_INDIVIDUAL_FIRST_NAME));
            individual.setLastName(extractString(cursor, COLUMN_INDIVIDUAL_LAST_NAME));
            individual.setDob(extractString(cursor, COLUMN_INDIVIDUAL_DOB));
            individual.setGender(extractString(cursor, COLUMN_INDIVIDUAL_GENDER));
            individual.setCurrentResidenceUuid(extractString(cursor, COLUMN_INDIVIDUAL_RESIDENCE_LOCATION_UUID));
            individual.setOtherId(extractString(cursor, COLUMN_INDIVIDUAL_OTHER_ID));
            individual.setOtherNames(extractString(cursor, COLUMN_INDIVIDUAL_OTHER_NAMES));
            individual.setPhoneNumber(extractString(cursor, COLUMN_INDIVIDUAL_PHONE_NUMBER));
            individual.setOtherPhoneNumber(extractString(cursor, COLUMN_INDIVIDUAL_OTHER_PHONE_NUMBER));
            individual.setPointOfContactName(extractString(cursor, COLUMN_INDIVIDUAL_POINT_OF_CONTACT_NAME));
            individual.setStatus(extractString(cursor, COLUMN_INDIVIDUAL_STATUS));
            individual.setPointOfContactPhoneNumber(extractString(cursor, COLUMN_INDIVIDUAL_POINT_OF_CONTACT_PHONE_NUMBER));
            individual.setLanguagePreference(extractString(cursor, COLUMN_INDIVIDUAL_LANGUAGE_PREFERENCE));
            individual.setNationality(extractString(cursor, COLUMN_INDIVIDUAL_NATIONALITY));
            individual.setRelationshipToHead(extractString(cursor, COLUMN_INDIVIDUAL_RELATIONSHIP_TO_HEAD));
            individual.setAttrs(extractString(cursor, COLUMN_INDIVIDUAL_ATTRS));
            return individual;
        }

        @Override
        public ContentValues toContentValues(Individual individual) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_INDIVIDUAL_UUID, individual.getUuid());
            contentValues.put(COLUMN_INDIVIDUAL_EXTID, individual.getExtId());
            contentValues.put(COLUMN_INDIVIDUAL_FIRST_NAME, individual.getFirstName());
            contentValues.put(COLUMN_INDIVIDUAL_LAST_NAME, individual.getLastName());
            contentValues.put(COLUMN_INDIVIDUAL_DOB, individual.getDob());
            contentValues.put(COLUMN_INDIVIDUAL_GENDER, individual.getGender());
            contentValues.put(COLUMN_INDIVIDUAL_RESIDENCE_LOCATION_UUID, individual.getCurrentResidenceUuid());
            contentValues.put(COLUMN_INDIVIDUAL_OTHER_ID, individual.getOtherId());
            contentValues.put(COLUMN_INDIVIDUAL_OTHER_NAMES, individual.getOtherNames());
            contentValues.put(COLUMN_INDIVIDUAL_PHONE_NUMBER, individual.getPhoneNumber());
            contentValues.put(COLUMN_INDIVIDUAL_OTHER_PHONE_NUMBER, individual.getOtherPhoneNumber());
            contentValues.put(COLUMN_INDIVIDUAL_POINT_OF_CONTACT_NAME, individual.getPointOfContactName());
            contentValues.put(COLUMN_INDIVIDUAL_STATUS, individual.getStatus());
            contentValues.put(COLUMN_INDIVIDUAL_POINT_OF_CONTACT_PHONE_NUMBER, individual.getPointOfContactPhoneNumber());
            contentValues.put(COLUMN_INDIVIDUAL_LANGUAGE_PREFERENCE, individual.getLanguagePreference());
            contentValues.put(COLUMN_INDIVIDUAL_NATIONALITY, individual.getNationality());
            contentValues.put(COLUMN_INDIVIDUAL_RELATIONSHIP_TO_HEAD, individual.getRelationshipToHead());
            contentValues.put(COLUMN_INDIVIDUAL_ATTRS, individual.getAttrs());
            return contentValues;
        }

        @Override
        public String getId(Individual individual) {
            return individual.getUuid();
        }

        @Override
        public DataWrapper toDataWrapper(ContentResolver contentResolver, Individual individual, String level) {

            DataWrapper dataWrapper = new DataWrapper();
            dataWrapper.setUuid(individual.getUuid());
            dataWrapper.setExtId(individual.getExtId());
            dataWrapper.setName(getFullName(individual));
            dataWrapper.setCategory(level);

            // for Bioko add individual details to payload
            dataWrapper.getStringsPayload().put(R.string.individual_other_names_label, individual.getOtherNames());
            dataWrapper.getStringsPayload().put(R.string.individual_language_preference_label, individual.getLanguagePreference());

            return dataWrapper;
        }
    }
}
