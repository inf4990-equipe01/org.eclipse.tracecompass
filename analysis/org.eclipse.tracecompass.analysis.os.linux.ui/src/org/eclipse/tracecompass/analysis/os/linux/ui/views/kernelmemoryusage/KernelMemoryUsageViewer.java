/**********************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Samuel Gagnon - Initial implementation
 **********************************************************************/
package org.eclipse.tracecompass.analysis.os.linux.ui.views.kernelmemoryusage;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelmemoryusage.KernelMemoryAnalysisModule;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.linecharts.TmfCommonXLineChartViewer;
import org.swtchart.Chart;

/**
 * Memory usage view
 *
 * @author Samuel Gagnon
 * @author Wassim Nasrallah
 *
 */
public class KernelMemoryUsageViewer extends TmfCommonXLineChartViewer {

    private static final class MemoryFormat extends Format {
        /**
         *
         */
        private static final long serialVersionUID = 3934127385682676804L;
        private static final String KB = "KB"; //$NON-NLS-1$
        private static final String MB = "MB"; //$NON-NLS-1$
        private static final String GB = "GB"; //$NON-NLS-1$
        private static final String TB = "TB"; //$NON-NLS-1$
        private static final long KILO = 1024;
        private static final Format FORMAT = new DecimalFormat("#.###"); //$NON-NLS-1$

        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj instanceof Double) {
                Double value = (Double) obj;
                if (value == 0) {
                    return toAppendTo.append("0"); //$NON-NLS-1$
                }
                if (value > KILO * KILO * KILO * KILO) {
                    return toAppendTo.append(FORMAT.format(value / (KILO * KILO * KILO * KILO))).append(' ').append(TB);
                }
                if (value > KILO * KILO * KILO) {
                    return toAppendTo.append(FORMAT.format(value / (KILO * KILO * KILO))).append(' ').append(GB);
                }
                if (value > KILO * KILO) {
                    return toAppendTo.append(FORMAT.format(value / (KILO * KILO))).append(' ').append(MB);
                }
                return toAppendTo.append(FORMAT.format(value / (KILO))).append(' ').append(KB);
            }
            return toAppendTo;
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            // TODO Auto-generated method stub
            return null;
        }
    }

    private TmfStateSystemAnalysisModule fModule = null;

    private String fSelectedThread = "-1";  //$NON-NLS-1$
    private static final String TOTAL = "total"; //$NON-NLS-1$

    Map<String, Double> lowestValueMap;


    /**
     * Constructor
     *
     * @param parent
     *            parent view
     */
    public KernelMemoryUsageViewer(Composite parent) {
        super(parent, Messages.MemoryUsageViewer_title, Messages.MemoryUsageViewer_xAxis, Messages.MemoryUsageViewer_yAxis);
    }

    @Override
    protected void initializeDataSource() {
        ITmfTrace trace = getTrace();
        if (trace != null) {

            fModule = TmfTraceUtils.getAnalysisModuleOfClass(trace, TmfStateSystemAnalysisModule.class, KernelMemoryAnalysisModule.ID);
            if (fModule == null) {
                return;
            }
            fModule.schedule();
            Chart chart = getSwtChart();
            chart.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    chart.getAxisSet().getYAxis(0).getTick().setFormat(new MemoryFormat());
                }
            });
        }

        fModule.waitForInitialization();
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return;
        }

        ss.waitUntilBuilt();
        lowestValueMap = new HashMap<>();

        try {
            List<Integer> threadQuarkList = ss.getSubAttributes(-1, false);
            for (Integer threadQuark : threadQuarkList) {
                String tid = ss.getAttributeName(threadQuark);
                int lowestValueQuark = ss.getQuarkRelative(threadQuark, KernelMemoryAnalysisModule.THREAD_LOWEST_MEMORY_VALUE);
                // The final state is the lowest memory value of the thread
                long ts = ss.getCurrentEndTime();
                ITmfStateInterval lowestValueState = ss.querySingleState(ts, lowestValueQuark);
                long lowestValue = lowestValueState.getStateValue().unboxLong();

                lowestValueMap.put(tid, (double) lowestValue);
            }

            double totalIncrementation = lowestValueMap.values().stream().mapToDouble(Double::doubleValue).sum();
            lowestValueMap.put(TOTAL, totalIncrementation);

        } catch (AttributeNotFoundException e) {
            Activator.getDefault().logError(e.getMessage(), e);
        } catch (StateSystemDisposedException e2) {
            Activator.getDefault().logError(e2.getMessage(), e2);
        }

    }

    @Override
    protected void updateData(long start, long end, int nb, IProgressMonitor monitor) {
        if (getTrace() == null || fModule == null) {
            return;
        }

        fModule.waitForInitialization();
        ITmfStateSystem ss = fModule.getStateSystem();
        /* Don't wait for the module completion, when it's ready, we'll know */
        if (ss == null) {
            return;
        }

        double[] xvalues = getXAxis(start, end, nb);
        if (xvalues.length == 0) {
            return;
        }
        setXAxis(xvalues);

        ss.waitUntilBuilt();

        try {
            /**
             * For a given time range, we plot two lines representing the memory allocation.
             * The first line represent the total memory allocation of every process.
             * The second line represent the memory allocation of the selected thread.
             */
            double [] totalKernelMemoryValues = new double[xvalues.length];
            double [] selectedThreadValues = new double[xvalues.length];
            for(int i = 0; i < xvalues.length; i++) {
                if (monitor.isCanceled()) {
                    return;
                }

                double x = xvalues[i];
                long t = (long)x + this.getTimeOffset();

                List<ITmfStateInterval> kernelState = ss.queryFullState(t);

                List<Integer> threadQuarkList = ss.getSubAttributes(-1, false);
                for (Integer threadQuark : threadQuarkList) {
                    ITmfStateInterval threadMemoryInterval = kernelState.get(threadQuark);

                    long value = threadMemoryInterval.getStateValue().unboxLong();
                    totalKernelMemoryValues[i] += value;

                    String tid = ss.getAttributeName(threadQuark);
                    if (tid.equals(fSelectedThread)) {
                        selectedThreadValues[i] = value;
                    }
                }
            }

            // We shift the total consumption up to avoid negative values.
            for (int i = 0; i < xvalues.length; i++) {
                totalKernelMemoryValues[i] -= lowestValueMap.getOrDefault(TOTAL, (double) 0);
                selectedThreadValues[i] -= lowestValueMap.getOrDefault(fSelectedThread, (double) 0);
            }
            setSeries(Messages.MemoryUsageViewer_Total, totalKernelMemoryValues);
            setSeries(Messages.SelectedThread_Value, selectedThreadValues);
            updateDisplay();

        } catch (StateSystemDisposedException e1) {
            Activator.getDefault().logError(e1.getMessage(), e1);
        } catch (AttributeNotFoundException e2) {
            Activator.getDefault().logError(e2.getMessage(), e2);
        }

    }

    /**
     * Set the selected thread ID, which will be graphed in this viewer
     *
     * @param tid
     *            The selected thread ID
     */
    public void setSelectedThread(String tid) {
        cancelUpdate();
        deleteSeries(tid);
        fSelectedThread = tid;
        updateContent();
    }

}
