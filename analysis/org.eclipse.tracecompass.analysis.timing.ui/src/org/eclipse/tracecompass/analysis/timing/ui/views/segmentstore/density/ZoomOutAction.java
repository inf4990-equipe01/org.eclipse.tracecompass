/******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.density;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.AnalysisTimingImageConstants;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.swtchart.Range;
import org.eclipse.tracecompass.internal.analysis.timing.ui.Activator;

/**
 * Zoom action for the density view
 */
class ZoomOutAction extends Action {

    private final AbstractSegmentStoreDensityView fView;

    /**
     * Constructors a ZoomOutAction.
     *
     * @param densityViewer
     *            The parent density viewer
     */
    public ZoomOutAction(AbstractSegmentStoreDensityView densityViewer) {
        fView = densityViewer;
    }

    @Override
    public void run() {
        final AbstractSegmentStoreDensityViewer chart = fView.getDensityViewer();
        if (chart != null) {
            chart.zoom(new Range(0, Long.MAX_VALUE));
        }
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return NonNullUtils.checkNotNull(Activator.getDefault().getImageDescripterFromPath(AnalysisTimingImageConstants.IMG_UI_ZOOM_OUT_MENU));
    }

    @Override
    public String getToolTipText() {
        return NonNullUtils.checkNotNull(Messages.AbstractSegmentStoreDensityViewer_ZoomOutActionToolTipText);
    }
}