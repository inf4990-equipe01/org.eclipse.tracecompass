/******************************************************************************
 * Copyright (c) 2015, 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.density;

import static org.eclipse.tracecompass.common.core.NonNullUtils.nullToEmptyString;

import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisModule;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.IAnalysisProgressListener;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.SubSecondTimeWithUnitFormat;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.segmentstore.density.MouseDragZoomProvider;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.segmentstore.density.MouseSelectionProvider;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.segmentstore.density.SimpleTooltipProvider;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentComparators;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IBarSeries;
import org.swtchart.ISeries;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.ISeriesSet;
import org.swtchart.LineStyle;
import org.swtchart.Range;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Displays the segment store analysis data in a density chart.
 *
 * @author Matthew Khouzam
 * @author Marc-Andre Laperle
 *
 * @since 2.0
 */
public abstract class AbstractSegmentStoreDensityViewer extends TmfViewer {

    private static final Format DENSITY_TIME_FORMATTER = new SubSecondTimeWithUnitFormat();
    private static final RGB BAR_COLOR = new RGB(0x42, 0x85, 0xf4);
    private final Chart fChart;
    private final MouseDragZoomProvider fDragZoomProvider;
    private final MouseSelectionProvider fDragProvider;
    private final SimpleTooltipProvider fTooltipProvider;

    private @Nullable ITmfTrace fTrace;
    private @Nullable IAnalysisProgressListener fListener;
    private @Nullable AbstractSegmentStoreAnalysisModule fAnalysisModule;
    private TmfTimeRange fCurrentTimeRange = TmfTimeRange.NULL_RANGE;
    private List<ISegmentStoreDensityViewerDataListener> fListeners;

    /**
     * Constructs a new density viewer.
     *
     * @param parent
     *            the parent of the viewer
     */
    public AbstractSegmentStoreDensityViewer(Composite parent) {
        super(parent);
        fListeners = new ArrayList<>();
        fChart = new Chart(parent, SWT.NONE);
        fChart.getLegend().setVisible(false);
        fChart.getTitle().setVisible(false);
        fChart.getAxisSet().getXAxis(0).getTitle().setText(nullToEmptyString(Messages.AbstractSegmentStoreDensityViewer_TimeAxisLabel));
        fChart.getAxisSet().getYAxis(0).getTitle().setText(nullToEmptyString(Messages.AbstractSegmentStoreDensityViewer_CountAxisLabel));
        fChart.getAxisSet().getXAxis(0).getGrid().setStyle(LineStyle.DOT);
        fChart.getAxisSet().getYAxis(0).getGrid().setStyle(LineStyle.DOT);

        fDragZoomProvider = new MouseDragZoomProvider(this);
        fDragZoomProvider.register();
        fDragProvider = new MouseSelectionProvider(this);
        fDragProvider.register();
        fTooltipProvider = new SimpleTooltipProvider(this);
        fTooltipProvider.register();
    }

    /**
     * Returns the segment store analysis module
     *
     * @param trace
     *            The trace to consider
     * @return the analysis module
     */
    protected @Nullable abstract AbstractSegmentStoreAnalysisModule getSegmentStoreAnalysisModule(ITmfTrace trace);

    @Nullable
    private static ITmfTrace getTrace() {
        return TmfTraceManager.getInstance().getActiveTrace();
    }

    private void updateDisplay(List<ISegment> data) {
        if (data.isEmpty()) {
            return;
        }
        IBarSeries series = (IBarSeries) fChart.getSeriesSet().createSeries(SeriesType.BAR, Messages.AbstractSegmentStoreDensityViewer_SeriesLabel);
        series.setVisible(true);
        series.setBarPadding(0);

        series.setBarColor(new Color(Display.getDefault(), BAR_COLOR));
        int barWidth = 4;
        final int width = fChart.getPlotArea().getBounds().width / barWidth;
        double[] xOrigSeries = new double[width];
        double[] yOrigSeries = new double[width];
        Arrays.fill(yOrigSeries, 1.0);
        long maxLength = data.get(data.size() - 1).getLength();
        double maxFactor = 1.0 / (maxLength + 1.0);
        long minX = Long.MAX_VALUE;
        for (ISegment segment : data) {
            double xBox = segment.getLength() * maxFactor * width;
            yOrigSeries[(int) xBox]++;
            minX = Math.min(minX, segment.getLength());
        }
        double timeWidth = (double) maxLength / (double) width;
        for (int i = 0; i < width; i++) {
            xOrigSeries[i] = i * timeWidth;
        }
        double maxY = Double.MIN_VALUE;
        for (int i = 0; i < width; i++) {
            maxY = Math.max(maxY, yOrigSeries[i]);
        }
        if (minX == maxLength) {
            maxLength++;
            minX--;
        }
        series.setYSeries(yOrigSeries);
        series.setXSeries(xOrigSeries);
        final IAxis xAxis = fChart.getAxisSet().getXAxis(0);
        /*
         * adjustrange appears to bring origin back since we pad the series with
         * 0s, not interesting.
         */
        xAxis.adjustRange();
        Range range = xAxis.getRange();
        // fix for overly aggressive lower after an adjust range
        range.lower = minX - range.upper + maxLength;
        xAxis.setRange(range);
        xAxis.getTick().setFormat(DENSITY_TIME_FORMATTER);
        fChart.getAxisSet().getYAxis(0).setRange(new Range(1.0, maxY));
        fChart.getAxisSet().getYAxis(0).enableLogScale(true);
        fChart.redraw();
    }

    @Override
    public Chart getControl() {
        return fChart;
    }

    /**
     * Select a range of latency durations in the viewer.
     *
     * @param durationRange
     *            a range of latency durations
     */
    public void select(Range durationRange) {
        computeDataAsync(fCurrentTimeRange, durationRange).thenAccept((data) -> {
            for (ISegmentStoreDensityViewerDataListener listener : fListeners) {
                listener.dataSelectionChanged(data);
            }
        });
    }

    /**
     * Zoom to a range of latency durations in the viewer.
     *
     * @param durationRange
     *            a range of latency durations
     */
    public void zoom(Range durationRange) {
        computeDataAsync(fCurrentTimeRange, durationRange).thenAccept((data) -> applyData(data));
    }

    private CompletableFuture<List<ISegment>> computeDataAsync(final TmfTimeRange timeRange, final Range durationRange) {
        return CompletableFuture.supplyAsync(() -> computeData(timeRange, durationRange));
    }

    private @Nullable List<ISegment> computeData(final TmfTimeRange timeRange, final Range durationRange) {
        final AbstractSegmentStoreAnalysisModule analysisModule = fAnalysisModule;
        if (analysisModule == null) {
            return null;
        }
        final ISegmentStore<ISegment> results = analysisModule.getResults();
        if (results == null) {
            return null;
        }

        Iterator<ISegment> intersectingElements = results.getIntersectingElements(timeRange.getStartTime().getValue(), timeRange.getEndTime().getValue()).iterator();

        if (durationRange.lower > Double.MIN_VALUE || durationRange.upper < Double.MAX_VALUE) {
            Predicate<? super ISegment> predicate = new Predicate<ISegment>() {
                @Override
                public boolean apply(@Nullable ISegment input) {
                    return input != null && input.getLength() >= durationRange.lower && input.getLength() <= durationRange.upper;
                }
            };
            intersectingElements = Iterators.<ISegment> filter(intersectingElements, predicate);
        }

        return Lists.newArrayList(intersectingElements);
    }

    private void applyData(final @Nullable List<ISegment> data) {
        if (data != null) {
            Collections.sort(data, SegmentComparators.INTERVAL_LENGTH_COMPARATOR);
            Display.getDefault().asyncExec(() -> updateDisplay(data));
            for (ISegmentStoreDensityViewerDataListener l : fListeners) {
                l.dataChanged(data);
            }
        }
    }

    /**
     * Signal handler for handling of the window range signal.
     *
     * @param signal
     *            The {@link TmfWindowRangeUpdatedSignal}
     */
    @TmfSignalHandler
    public void windowRangeUpdated(@Nullable TmfWindowRangeUpdatedSignal signal) {
        if (signal == null) {
            return;
        }
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return;
        }
        fAnalysisModule = getSegmentStoreAnalysisModule(trace);
        fCurrentTimeRange = NonNullUtils.checkNotNull(signal.getCurrentRange());
        updateWithRange(fCurrentTimeRange);
    }

    private void updateWithRange(final TmfTimeRange range) {
        computeDataAsync(range, new Range(Double.MIN_VALUE, Double.MAX_VALUE)).thenAccept((data) -> applyData(data));
    }

    @Override
    public void refresh() {
        fChart.redraw();
    }

    @Override
    public void dispose() {
        if (fAnalysisModule != null && fListener != null) {
            fAnalysisModule.removeListener(fListener);
        }
        fDragZoomProvider.deregister();
        fTooltipProvider.deregister();
        fDragProvider.deregister();
        super.dispose();
    }

    /**
     * Signal handler for handling of the trace opened signal.
     *
     * @param signal
     *            The trace opened signal {@link TmfTraceOpenedSignal}
     */
    @TmfSignalHandler
    public void traceOpened(TmfTraceOpenedSignal signal) {
        fTrace = signal.getTrace();
        loadTrace(getTrace());
    }

    /**
     * Signal handler for handling of the trace selected signal.
     *
     * @param signal
     *            The trace selected signal {@link TmfTraceSelectedSignal}
     */
    @TmfSignalHandler
    public void traceSelected(TmfTraceSelectedSignal signal) {
        if (fTrace != signal.getTrace()) {
            fTrace = signal.getTrace();
            loadTrace(getTrace());
        }
    }

    /**
     * Signal handler for handling of the trace closed signal.
     *
     * @param signal
     *            The trace closed signal {@link TmfTraceClosedSignal}
     */
    @TmfSignalHandler
    public void traceClosed(TmfTraceClosedSignal signal) {

        if (signal.getTrace() != fTrace) {
            return;
        }

        fTrace = null;
        clearContent();
    }

    /**
     * A Method to load a trace into the viewer.
     *
     * @param trace
     *            A trace to apply in the viewer
     */
    protected void loadTrace(@Nullable ITmfTrace trace) {
        clearContent();

        fTrace = trace;
        TmfTraceContext ctx = TmfTraceManager.getInstance().getCurrentTraceContext();
        TmfTimeRange windowRange = ctx.getWindowRange();
        fCurrentTimeRange = windowRange;

        if (trace != null) {
            fAnalysisModule = getSegmentStoreAnalysisModule(trace);
            final AbstractSegmentStoreAnalysisModule module = fAnalysisModule;
            if (module != null) {
                fListener = (activeAnalysis, data) -> updateWithRange(windowRange);
                module.addListener(fListener);
                module.schedule();
            }
        }
        zoom(new Range(0, Long.MAX_VALUE));
    }

    /**
     * Clears the view content.
     */
    private void clearContent() {
        final Chart chart = fChart;
        if (!chart.isDisposed()) {
            ISeriesSet set = chart.getSeriesSet();
            ISeries[] series = set.getSeries();
            for (int i = 0; i < series.length; i++) {
                set.deleteSeries(series[i].getId());
            }
            for (IAxis axis : chart.getAxisSet().getAxes()) {
                axis.setRange(new Range(0, 1));
            }
            chart.redraw();
        }
    }

    /**
     * Add a data listener.
     *
     * @param dataListener
     *            the data listener to add
     */
    public void addDataListener(ISegmentStoreDensityViewerDataListener dataListener) {
        fListeners.add(dataListener);
    }

    /**
     * Remove a data listener.
     *
     * @param dataListener
     *            the data listener to remove
     */
    public void removeDataListener(ISegmentStoreDensityViewerDataListener dataListener) {
        fListeners.remove(dataListener);
    }
}