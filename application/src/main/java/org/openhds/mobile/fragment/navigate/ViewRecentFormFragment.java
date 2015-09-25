package org.openhds.mobile.fragment.navigate;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.openhds.mobile.R;
import org.openhds.mobile.adapter.FormInstanceAdapter;
import org.openhds.mobile.model.form.FormInstance;
import org.openhds.mobile.utilities.EncryptionHelper;
import org.openhds.mobile.utilities.OdkCollectHelper;
import static org.openhds.mobile.utilities.MessageUtils.showShortToast;


public class ViewRecentFormFragment extends Fragment implements View.OnClickListener
{
    Button recentForm;
    private List<FormInstance> recentformInstances;
    private List<FormInstance> recentformInstancesCategorized ;

    String currentModuleName;
    private ListView recentFormInstanceView;

    public void setCurrentModuleName(String currentModuleName) {
        this.currentModuleName = currentModuleName;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.view_form_fragment, container, false);
        recentForm = (Button) view.findViewById(R.id.viewRecentFormButton);
        recentForm.setOnClickListener(this);
        return view;
    }


    public void onClick(View view) {

        populateRecentFormInstanceListView();
    }


    public void populateRecentFormInstanceListView() {

        recentformInstances = OdkCollectHelper.getAllUnsentFormInstances(getActivity().getContentResolver());
        recentFormInstanceView =  (ListView) getActivity().findViewById(R.id.view_recent_form_right_column);

        if (null == recentformInstances ||recentformInstances.isEmpty()) {
            return;
        }

        recentformInstancesCategorized=checkRecentFormByName(recentformInstances, currentModuleName);

        FormInstanceAdapter adapter = new FormInstanceAdapter(
                getActivity().getApplicationContext(), R.id.form_instance_list_item, recentformInstancesCategorized.toArray());

        recentFormInstanceView.setAdapter(adapter);
        recentFormInstanceView.setOnItemClickListener(new RecentFormInstanceClickListener());
    }

//checks the form instances by name and populate it based on the current module

    public static List<FormInstance>checkRecentFormByName(List<FormInstance> recentformInstances, String currentModuleName)
    {
          List <FormInstance> recentformInstancesCategorized = new ArrayList<FormInstance>();

          Iterator<FormInstance> iterator = recentformInstances.iterator();
          while(iterator.hasNext())
          {
              FormInstance instance = iterator.next();

              if ((currentModuleName.equals("CensusActivityModule")) && ((instance.getFormName().equals("location") ||
                      instance.getFileName().equals("individual")))) {
                  recentformInstancesCategorized.add(instance);
              }

              else if ((currentModuleName.equals("BiokoActivityModule")) && ((instance.getFormName().equals("bed_net") ||
                      instance.getFormName().equals("spraying") || instance.getFormName().equals("super_ojo")))){

                       recentformInstancesCategorized.add(instance);
              }
              else if ((currentModuleName.equals("UpdateActivityModule")) && (instance.getFormName().equals("visit") ||
                        instance.getFormName().equals("pregnancy_observation")|| instance.getFormName().equals("in_migration")||
                        instance.getFormName().equals("out_migration")|| instance.getFormName().equals("pregnancy_outcome")))
                {
                      recentformInstancesCategorized.add(instance);

                }
          }

        Collections.reverse(recentformInstancesCategorized);
        return recentformInstancesCategorized;

    }

    //Sort the list in

// Launch an intent for ODK Collect when user clicks on a form instance.
    private class RecentFormInstanceClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (position >= 0 && position < recentformInstancesCategorized.size()) {
                FormInstance selected = recentformInstancesCategorized.get(position);
                Uri uri = Uri.parse(selected.getUriString());

                File selectedFile = new File(selected.getFilePath());
                EncryptionHelper.decryptFile(selectedFile, getActivity().getApplicationContext());

                Intent intent = new Intent(Intent.ACTION_EDIT, uri);
                showShortToast(getActivity().getApplicationContext(), R.string.launching_odk_collect);
                startActivityForResult(intent, 0);
            }
        }
    }


}
