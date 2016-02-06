/**********************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Samuel Gagnon & Mahdi Zolnouri & Wassim Nasrallah - Initial implementation
 **********************************************************************/
package org.eclipse.tracecompass.analysis.os.linux.ui.views.kernelmemoryusage;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentInfo;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentSignal;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.views.TmfChartView;

/**
 * Memory usage view
 *
 * @author Samuel Gagnon
 * @author Mahdi Zolnouri
 * @author Wassim Nasrallah
 */
public class KernelMemoryUsageView extends TmfChartView {
    private static final int[] DEFAULT_WEIGHTS = {1, 3};

    private KernelMemoryUsageComposite fTreeViewer = null;
    private KernelMemoryUsageViewer fXYViewer = null;

    private SashForm fSashForm;
    private Listener fSashDragListener;
    /** A composite that allows us to add margins */
    private Composite fXYViewerContainer;

    /** ID string */
    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.ui.kernelmemoryusageview"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public KernelMemoryUsageView() {
        super(Messages.MemoryUsageView_title);
    }

    @Override
    public TmfTimeViewAlignmentInfo getTimeViewAlignmentInfo() {
        if (fSashForm == null) {
            return null;
        }

        return new TmfTimeViewAlignmentInfo(fSashForm.getShell(), fSashForm.toDisplay(0, 0), getTimeAxisOffset());
    }

    private int getTimeAxisOffset() {
        return fTreeViewer.getControl().getSize().x + fSashForm.getSashWidth() + fXYViewer.getPointAreaOffset();
    }



    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        return new KernelMemoryUsageViewer(parent);
    }
    @Override
    public void createPartControl(Composite parent) {
        fSashForm = new SashForm(parent, SWT.NONE);

        fTreeViewer = new KernelMemoryUsageComposite(fSashForm);

        fXYViewerContainer = new Composite(fSashForm, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        fXYViewerContainer.setLayout(layout);

        /* Build the XY chart part of the view */
        fXYViewer = new KernelMemoryUsageViewer(fXYViewerContainer);
        fXYViewer.setSendTimeAlignSignals(true);
        fXYViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        /* Add selection listener to tree viewer */
        fTreeViewer.addSelectionChangeListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    Object structSelection = ((IStructuredSelection) selection).getFirstElement();
                    if (structSelection instanceof KernelMemoryUsageEntry) {
                        KernelMemoryUsageEntry entry = (KernelMemoryUsageEntry) structSelection;
                        fTreeViewer.setSelectedThread(entry.getTid());
                        fXYViewer.setSelectedThread(entry.getTid());
                    }
                }
            }
        });

        /* Initialize the viewers with the currently selected trace */
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            TmfTraceSelectedSignal signal = new TmfTraceSelectedSignal(this, trace);
            fTreeViewer.traceSelected(signal);
            fXYViewer.traceSelected(signal);
        }
        fTreeViewer.getControl().addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                super.controlResized(e);
            }
        });

        fXYViewer.getControl().addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                // Sashes in a SashForm are being created on layout so add the
                // drag listener here
                if (fSashDragListener == null) {
                    for (Control control : fSashForm.getChildren()) {
                        if (control instanceof Sash) {
                            fSashDragListener = new Listener() {

                                @Override
                                public void handleEvent(Event event) {
                                    TmfSignalManager.dispatchSignal(new TmfTimeViewAlignmentSignal(fSashForm, getTimeViewAlignmentInfo()));
                                }
                            };
                            control.removePaintListener(this);
                            control.addListener(SWT.Selection, fSashDragListener);
                            // There should be only one sash
                            break;
                        }
                    }
                }
            }
        });

        fSashForm.setWeights(DEFAULT_WEIGHTS);
    }
    @Override
    public void setFocus() {
        fXYViewer.getControl().setFocus();
    }
    @Override
    public void dispose() {
        super.dispose();
        if (fTreeViewer != null) {
            fTreeViewer.dispose();
        }
        if (fXYViewer != null) {
            fXYViewer.dispose();
        }
    }

}
