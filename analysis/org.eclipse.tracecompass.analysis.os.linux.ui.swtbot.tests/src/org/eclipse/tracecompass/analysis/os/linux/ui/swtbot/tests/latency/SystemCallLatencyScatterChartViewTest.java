/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.ui.swtbot.tests.latency;

import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertVisible;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBotControl;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.latency.SystemCallLatencyScatterView;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.ConditionHelpers;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.swtchart.Chart;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries;
import org.swtchart.ISeriesSet;
import org.swtchart.Range;

/**
 * Tests of the scatter chart view
 *
 * @author Matthew Khouzam
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class SystemCallLatencyScatterChartViewTest {

    private static final String TRACE_TYPE = "org.eclipse.linuxtools.lttng2.kernel.tracetype";
    private static final String PROJECT_NAME = "test";
    private static final String VIEW_ID = SystemCallLatencyScatterView.ID;

    /** The Log4j logger instance. */
    private static final Logger fLogger = Logger.getRootLogger();
    private Chart fScatterChart;

    /**
     * Things to setup
     */
    @BeforeClass
    public static void beforeClass() {

        SWTBotUtils.initialize();
        Thread.currentThread().setName("SWTBotTest");
        /* set up for swtbot */
        SWTBotPreferences.TIMEOUT = 20000; /* 20 second timeout */
        SWTBotPreferences.KEYBOARD_LAYOUT = "EN_US";
        fLogger.removeAllAppenders();
        fLogger.addAppender(new ConsoleAppender(new SimpleLayout(), ConsoleAppender.SYSTEM_OUT));
        SWTWorkbenchBot bot = new SWTWorkbenchBot();
        SWTBotUtils.closeView("welcome", bot);
        /* Switch perspectives */
        SWTBotUtils.switchToTracingPerspective();
        /* Finish waiting for eclipse to load */
        SWTBotUtils.waitForJobs();

    }

    /**
     * Opens a latency scatter chart
     *
     * @throws SecurityException
     *             If a security manager is present and any the wrong class is
     *             loaded or the class loader is not the same as its ancestor's
     *             loader.
     *
     * @throws IllegalArgumentException
     *             the object is not the correct class type
     *
     *
     */
    @Before
    public void createScatterViewer() throws SecurityException, IllegalArgumentException {
        /*
         * Open latency view
         */
        SWTBotUtils.openView(VIEW_ID);
        SWTWorkbenchBot bot = new SWTWorkbenchBot();
        SWTBotView viewBot = bot.viewById(VIEW_ID);
        final IViewReference viewReference = viewBot.getViewReference();
        IViewPart viewPart = UIThreadRunnable.syncExec(new Result<IViewPart>() {
            @Override
            public IViewPart run() {
                return viewReference.getView(true);
            }
        });
        assertNotNull(viewPart);
        if (!(viewPart instanceof SystemCallLatencyScatterView)) {
            fail("Could not instanciate view");
        }
        fScatterChart = viewBot.bot().widget(WidgetOfType.widgetOfType(Chart.class));
        assertNotNull(fScatterChart);
    }

    /**
     * Closes the view
     */
    @After
    public void closeDensityViewer() {
        final SWTWorkbenchBot swtWorkbenchBot = new SWTWorkbenchBot();
        SWTBotView viewBot = swtWorkbenchBot.viewById(VIEW_ID);
        viewBot.close();
    }

    /**
     * Test with an actual trace, this is more of an integration test than a
     * unit test. This test is a slow one too. If some analyses are not well
     * configured, this test will also generates null pointer exceptions. These
     * will be logged.
     *
     * @throws IOException
     *             trace not found?
     * @throws SecurityException
     *             If a security manager is present and any the wrong class is
     *             loaded or the class loader is not the same as its ancestor's
     *             loader.
     *
     * @throws IllegalArgumentException
     *             the object is not the correct class type
     *
     */
    @Test
    public void testWithTrace() throws IOException, SecurityException, IllegalArgumentException {
        String tracePath;
        tracePath = FileLocator.toFileURL(CtfTestTrace.ARM_64_BIT_HEADER.getTraceURL()).getPath();
        SWTWorkbenchBot bot = new SWTWorkbenchBot();
        SWTBotView view = bot.viewById(VIEW_ID);
        view.close();
        bot.waitUntil(ConditionHelpers.ViewIsClosed(view));
        SWTBotUtils.createProject(PROJECT_NAME);
        SWTBotUtils.openTrace(PROJECT_NAME, tracePath, TRACE_TYPE);
        SWTBotUtils.waitForJobs();
        createScatterViewer();
        SWTBotUtils.waitForJobs();

        final Chart scatterChart = fScatterChart;
        assertNotNull(scatterChart);
        SWTBotChart chartBot = new SWTBotChart(scatterChart);
        assertVisible(chartBot);
        assertEquals("", chartBot.getToolTipText());
        final Range range = scatterChart.getAxisSet().getXAxes()[0].getRange();
        assertEquals(100000000, range.upper - range.lower, 0);
        ISeriesSet seriesSet = fScatterChart.getSeriesSet();
        assertNotNull(seriesSet);
        ISeries[] series = seriesSet.getSeries();
        assertNotNull(series);

        // Verify that the chart has 1 series
        assertEquals(1, series.length);
        // Verify that each series is a ILineSeries
        for (int i = 0; i < series.length; i++) {
            assertTrue(series[i] instanceof ILineSeries);
        }
        bot.closeAllEditors();
        SWTBotUtils.deleteProject(PROJECT_NAME, bot);
    }

    private static class SWTBotChart extends AbstractSWTBotControl<Chart> {
        public SWTBotChart(Chart w) throws WidgetNotFoundException {
            super(w);
        }
    }
}
