/******************************************************************************
 * Copyright (c) 2015, 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.density;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.table.AbstractSegmentStoreTableViewer;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

/**
 * Displays the segment store analysis data in a density chart and a table
 * corresponding to the selected latencies.
 *
 * @author Matthew Khouzam
 * @author Marc-Andre Laperle
 */
public abstract class AbstractSegmentStoreDensityView extends TmfView {

    private static final int[] DEFAULT_WEIGHTS = new int[] { 4, 6 };

    private @Nullable AbstractSegmentStoreDensityViewer fDensityViewer;
    private @Nullable AbstractSegmentStoreTableViewer fTableViewer;

    /**
     * Constructs a segment store density view
     *
     * @param viewName
     *            the name of the view
     */
    public AbstractSegmentStoreDensityView(String viewName) {
        super(viewName);
    }

    /**
     * Used to keep the table in sync with the density viewer.
     */
    private final class DataChangedListener implements ISegmentStoreDensityViewerDataListener {
        @Override
        public void dataChanged(List<ISegment> data) {
            updateTableModel(data);
        }

        private void updateTableModel(@Nullable List<ISegment> data) {
            final AbstractSegmentStoreTableViewer viewer = fTableViewer;
            if (viewer != null && data != null) {
                viewer.updateModel(data.toArray(new ISegment[] {}));
            }
        }

        @Override
        public void dataSelectionChanged(List<ISegment> data) {
            updateTableModel(data);
        }
    }

    @Override
    public void createPartControl(@Nullable Composite parent) {
        super.createPartControl(parent);

        final SashForm sashForm = new SashForm(parent, SWT.NONE);

        fTableViewer = createSegmentStoreTableViewer(sashForm);
        fDensityViewer = createSegmentStoreDensityViewer(sashForm);
        fDensityViewer.addDataListener(new DataChangedListener());

        sashForm.setWeights(DEFAULT_WEIGHTS);

        Action zoomOut = new ZoomOutAction(this);
        IToolBarManager toolBar = getViewSite().getActionBars().getToolBarManager();
        toolBar.add(zoomOut);
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null && fDensityViewer != null) {
            fDensityViewer.loadTrace(trace);
        }
    }

    /**
     * Create a table viewer suitable for displaying the segment store content.
     *
     * @param parent
     *            the parent composite
     * @return the table viewer
     */
    protected abstract AbstractSegmentStoreTableViewer createSegmentStoreTableViewer(Composite parent);

    /**
     * Create a density viewer suitable for displaying the segment store
     * content.
     *
     * @param parent
     *            the parent composite
     * @return the density viewer
     */
    protected abstract AbstractSegmentStoreDensityViewer createSegmentStoreDensityViewer(Composite parent);

    @Override
    public void setFocus() {
        final AbstractSegmentStoreDensityViewer viewer = fDensityViewer;
        if (viewer != null) {
            viewer.getControl().setFocus();
        }
    }

    @Override
    public void dispose() {
        final AbstractSegmentStoreDensityViewer densityViewer = fDensityViewer;
        if (densityViewer != null) {
            densityViewer.dispose();
        }

        final AbstractSegmentStoreTableViewer tableViewer = fTableViewer;
        if (tableViewer != null) {
            tableViewer.dispose();
        }

        super.dispose();
    }

    // Package-visible on purpose for ZoomOutAction
    @Nullable
    AbstractSegmentStoreDensityViewer getDensityViewer() {
        return fDensityViewer;
    }
}