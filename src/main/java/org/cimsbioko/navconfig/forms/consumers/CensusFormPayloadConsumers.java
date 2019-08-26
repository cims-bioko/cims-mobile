package org.cimsbioko.navconfig.forms.consumers;

import android.content.ContentResolver;

import org.cimsbioko.model.core.Individual;
import org.cimsbioko.model.core.Location;
import org.cimsbioko.model.core.LocationHierarchy;
import org.cimsbioko.navconfig.ProjectFormFields;
import org.cimsbioko.navconfig.forms.LaunchContext;
import org.cimsbioko.navconfig.forms.UsedByJSConfig;
import org.cimsbioko.navconfig.forms.adapters.IndividualFormAdapter;
import org.cimsbioko.navconfig.forms.adapters.LocationFormAdapter;
import org.cimsbioko.repository.DataWrapper;
import org.cimsbioko.repository.GatewayRegistry;
import org.cimsbioko.repository.gateway.IndividualGateway;
import org.cimsbioko.repository.gateway.LocationGateway;
import org.cimsbioko.repository.gateway.LocationHierarchyGateway;
import org.cimsbioko.utilities.IdHelper;

import java.util.Map;

import static org.cimsbioko.navconfig.BiokoHierarchy.HOUSEHOLD;
import static org.cimsbioko.navconfig.BiokoHierarchy.SECTOR;
import static org.cimsbioko.navconfig.forms.builders.PayloadTools.flagForReview;

public class CensusFormPayloadConsumers {

    private static void ensureLocationSectorExists(Map<String, String> payload, ContentResolver resolver) {

        LocationHierarchyGateway gw = GatewayRegistry.getLocationHierarchyGateway();

        // lookup relevant values from the form
        String formMapUuid = payload.get(ProjectFormFields.Locations.HIERARCHY_PARENT_UUID);
        String formSectorUuid = payload.get(ProjectFormFields.Locations.HIERARCHY_UUID);
        String formSectorName = payload.get(ProjectFormFields.Locations.SECTOR_NAME);

        // compute the sector's expected extid based on embedded map uuid and specified sector name
        LocationHierarchy mapArea = gw.getFirst(resolver, gw.findById(formMapUuid));
        String computedSectorExtId = mapArea.getExtId().replaceFirst("^(M\\d+)\\b", "$1\\" + formSectorName);

        // lookup the sectors by expected extid and embedded sector uuid
        LocationHierarchy sectorByUuid = gw.getFirst(resolver, gw.findById(formSectorUuid));
        LocationHierarchy sectorByComputedExtId = gw.getFirst(resolver, gw.findByExtId(computedSectorExtId));

        boolean sectorNeedsUpdate = sectorByUuid == null || !computedSectorExtId.equals(sectorByUuid.getExtId());

        if (sectorNeedsUpdate && sectorByComputedExtId == null) {
            sectorByComputedExtId = new LocationHierarchy();
            sectorByComputedExtId.setUuid(IdHelper.generateEntityUuid());
            sectorByComputedExtId.setParentUuid(mapArea.getUuid());
            sectorByComputedExtId.setExtId(computedSectorExtId);
            sectorByComputedExtId.setName(formSectorName);
            sectorByComputedExtId.setLevel(SECTOR);
            gw.insertOrUpdate(resolver, sectorByComputedExtId);
        }

        if (sectorNeedsUpdate) {
            payload.put(ProjectFormFields.Locations.HIERARCHY_UUID, sectorByComputedExtId.getUuid());
            payload.put(ProjectFormFields.Locations.HIERARCHY_PARENT_UUID, sectorByComputedExtId.getParentUuid());
            payload.put(ProjectFormFields.Locations.HIERARCHY_EXTID, sectorByComputedExtId.getExtId());
            flagForReview(payload);
        }
    }

    private static Location insertOrUpdateLocation(Map<String, String> formPayload, ContentResolver contentResolver) {
        Location location = LocationFormAdapter.fromForm(formPayload);
        GatewayRegistry.getLocationGateway().insertOrUpdate(contentResolver, location);
        return location;
    }

    private static Individual insertOrUpdateIndividual(Map<String, String> formPayLoad, ContentResolver contentResolver) {
        Individual individual = IndividualFormAdapter.fromForm(formPayLoad);
        IndividualGateway individualGateway = GatewayRegistry.getIndividualGateway();
        individualGateway.insertOrUpdate(contentResolver, individual);
        return individual;
    }

    @UsedByJSConfig
    public static class AddLocation implements FormPayloadConsumer {

        @Override
        public ConsumerResult consumeFormPayload(Map<String, String> formPayload, LaunchContext ctx) {
            ContentResolver contentResolver = ctx.getContentResolver();
            ensureLocationSectorExists(formPayload, contentResolver);
            insertOrUpdateLocation(formPayload, contentResolver);
            return new ConsumerResult(true);
        }

        @Override
        public void augmentInstancePayload(Map<String, String> formPayload) {
            formPayload.put(ProjectFormFields.General.ENTITY_EXTID, formPayload.get(ProjectFormFields.Locations.LOCATION_EXTID));
        }
    }

    @UsedByJSConfig
    public static class AddMemberOfHousehold extends DefaultConsumer {

        @Override
        public ConsumerResult consumeFormPayload(Map<String, String> formPayload, LaunchContext ctx) {
            insertOrUpdateIndividual(formPayload, ctx.getContentResolver());
            return super.consumeFormPayload(formPayload, ctx);
        }
    }

    @UsedByJSConfig
    public static class AddHeadOfHousehold extends DefaultConsumer {

        @Override
        public ConsumerResult consumeFormPayload(Map<String, String> formPayload, LaunchContext ctx) {

            LocationGateway locationGateway = GatewayRegistry.getLocationGateway();
            ContentResolver contentResolver = ctx.getContentResolver();

            Individual individual = insertOrUpdateIndividual(formPayload, ctx.getContentResolver());

            // Update the name of the location with the head's last name
            DataWrapper selectedLocation = ctx.getHierarchyPath().get(HOUSEHOLD);
            Location location = locationGateway.getFirst(contentResolver, locationGateway.findById(selectedLocation.getUuid()));
            String locationName = individual.getLastName();
            location.setName(locationName);
            selectedLocation.setName(locationName);
            locationGateway.insertOrUpdate(contentResolver, location);

            return new ConsumerResult(true);
        }

        @Override
        public void augmentInstancePayload(Map<String, String> formPayload) {
            // head of the household is always "self" to the head of household
            formPayload.put(ProjectFormFields.Individuals.RELATIONSHIP_TO_HEAD, "1");
        }
    }
}