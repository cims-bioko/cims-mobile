package org.cimsbioko.navconfig.forms.filters;

import org.cimsbioko.navconfig.forms.LaunchContext;

/**
 * A default filter implementation to handle case where there should be no filtering.
 */
public class NullFilter implements FormFilter {

    public static final NullFilter INSTANCE = new NullFilter();

    @Override
    public boolean shouldDisplay(LaunchContext ctx) {
        return true;
    }
}