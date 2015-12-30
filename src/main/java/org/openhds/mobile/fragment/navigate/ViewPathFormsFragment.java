package org.openhds.mobile.fragment.navigate;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.openhds.mobile.R;
import org.openhds.mobile.adapter.FormInstanceAdapter;
import org.openhds.mobile.model.form.FormInstance;

import java.io.File;
import java.util.List;

import static org.openhds.mobile.utilities.MessageUtils.showShortToast;


public class ViewPathFormsFragment extends Fragment {

    private ListView instanceList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_form_fragment, container, false);
    }

    public void populateRecentFormInstanceListView(List<FormInstance> formsForPath) {
        FormInstanceAdapter adapter = new FormInstanceAdapter(getActivity(), R.id.form_instance_list_item, formsForPath.toArray());
        instanceList = (ListView) getActivity().findViewById(R.id.path_forms_form_right_column);
        instanceList.setAdapter(adapter);
        instanceList.setOnItemClickListener(new ClickListener());
    }

    private void launch(FormInstance instance, String action) {
        Uri uri = Uri.parse(instance.getUriString());
        Intent intent = new Intent(action, uri);
        startActivityForResult(intent, 0);
    }

    private void launchEdit(FormInstance selected) {
        launch(selected, Intent.ACTION_EDIT);
        showShortToast(getActivity(), R.string.launching_odk_collect);
    }

    private class ClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ListView listView = (ListView) parent;
            FormInstance selected = (FormInstance) listView.getAdapter().getItem(position);
            if (selected != null) {
                launchEdit(selected);
            }
        }
    }
}
