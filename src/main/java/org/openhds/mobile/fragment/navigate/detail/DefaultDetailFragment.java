package org.openhds.mobile.fragment.navigate.detail;

import org.openhds.mobile.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class DefaultDetailFragment extends DetailFragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return (LinearLayout) inflater.inflate(
				R.layout.default_detail_fragment, container, false);
	}

}
