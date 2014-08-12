package org.openhds.mobile.projectdata.FormPayloadBuilders;

import org.openhds.mobile.activity.NavigateActivity;
import org.openhds.mobile.projectdata.ProjectActivityBuilder.UpdateActivityModule;
import org.openhds.mobile.projectdata.ProjectFormFields;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

public class UpdateFormPayloadBuilders {

	/**
	 * 
	 * Helper methods for FormPayloadBuilders
	 * 
	 */

	public static class StartAVisit implements FormPayloadBuilder {

		@Override
		public void buildFormPayload(Map<String, String> formPayload,
				NavigateActivity navigateActivity) {

			PayloadTools.addMinimalFormPayload(formPayload, navigateActivity);
            PayloadTools.flagForReview(formPayload, false);

            String visitDate = new SimpleDateFormat("yyyy-MM-dd").format(
					Calendar.getInstance().getTime()).toString();
			String locationExtId = navigateActivity.getHierarchyPath()
					.get(UpdateActivityModule.HOUSEHOLD_STATE).getExtId();
			String visitExtId = visitDate + "_" + locationExtId;

			formPayload.put(ProjectFormFields.Visits.VISIT_DATE, visitDate);
			formPayload.put(ProjectFormFields.Visits.LOCATION_EXTID,
					locationExtId);
			formPayload.put(ProjectFormFields.Visits.VISIT_EXTID, visitExtId);
		}
	}

    public static class RegisterOutMigration implements FormPayloadBuilder {

        @Override
        public void buildFormPayload(Map<String, String> formPayload,
                                     NavigateActivity navigateActivity) {

            PayloadTools.addMinimalFormPayload(formPayload, navigateActivity);
            PayloadTools.flagForReview(formPayload, false);

            String outMigrationDate = new SimpleDateFormat("yyyy-MM-dd").format(
                    Calendar.getInstance().getTime()).toString();

            String individualExtId = navigateActivity.getHierarchyPath()
                    .get(UpdateActivityModule.INDIVIDUAL_STATE).getExtId();


            formPayload.put(ProjectFormFields.OutMigrations.OUT_MIGRATION_DATE, outMigrationDate);
            formPayload.put(ProjectFormFields.OutMigrations.OUT_MIGRATION_INDIVIDUAL_EXTID,
                    individualExtId);

            formPayload.put(ProjectFormFields.Visits.VISIT_EXTID, navigateActivity.getCurrentVisit().getVisitExtId());
        }
    }
}