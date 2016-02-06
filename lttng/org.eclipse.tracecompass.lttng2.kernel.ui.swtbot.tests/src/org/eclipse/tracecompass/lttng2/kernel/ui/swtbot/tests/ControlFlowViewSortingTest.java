/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.lttng2.kernel.ui.swtbot.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeColumn;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimePreferencesConstants;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.ConditionHelpers.SWTBotTestCondition;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils.Resolution;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils.TimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SWTBot tests for column sorting in the Control Flow view.
 *
 * @author Bernd Hufmann
 */
@SuppressWarnings("restriction")
@RunWith(SWTBotJunit4ClassRunner.class)
public class ControlFlowViewSortingTest extends KernelTestBase {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------
    private static final String FILTER_ACTION = "Show View Filters";
    private static final String FILTER_DIALOG_TITLE = "Filter";
    private static final String UNCHECK_ALL = "Uncheck all";
    private static final String CHECK_SUBTREE = "Check subtree";
    private static final String OK_BUTTON = "OK";

    private static final String PROCESS_COLUMN = "Process";
    private static final int PROCESS_COLUMN_ID = 0;
    private static final String TID_COLUMN = "TID";
    private static final int TID_COLUMN_ID = 1;
    private static final String PTID_COLUMN = "PTID";
    private static final String BIRTH_COLUMN = "Birth time";
    private static final int BIRTH_COLUMN_ID = 3;
    private static final String TRACE_COLUMN = "Trace";

    private static final String SYSTEMD_PROCESS_NAME = "systemd";
    private static final long SYSTEMD_BIRTHTIME = 1361214078967531336L;
    private static final String SYSTEMD_TID = "1";

    private static final String KTHREAD_PROCESS_NAME = "kthreadd";
    private static final long KTHREAD_BIRTHTIME = 1361214078967533536L;
    private static final String KTHREAD_TID = "2";

    private static final String LTTNG_CONSUMER_PROCESS_NAME = "lttng-consumerd";
    private static final long LTTNG_CONSUMER_BIRTHTIME = 1361214078963717040L;
    private static final String LTTNG_CONSUMER_TID = "4034";

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private SWTBotView fViewBot;

    // ------------------------------------------------------------------------
    // Housekeeping
    // ------------------------------------------------------------------------
    /**
     * Before Test
     */
    @Override
    @Before
    public void before() {

        try {
            IEclipsePreferences defaultPreferences = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
            defaultPreferences.put(ITmfTimePreferencesConstants.TIME_ZONE, "GMT-05:00");
            TmfTimestampFormat.updateDefaultFormats();

            String tracePath = FileLocator.toFileURL(CtfTestTrace.SYNC_DEST.getTraceURL()).getPath();
            SWTBotUtils.openTrace(TRACE_PROJECT_NAME, tracePath, KERNEL_TRACE_TYPE);
            fViewBot = fBot.viewByTitle("Control Flow");
            fViewBot.show();
            fViewBot.setFocus();
        } catch (IOException e) {
            fail();
        }
    }

    @Override
    public void after() {
        IEclipsePreferences defaultPreferences = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        defaultPreferences.put(ITmfTimePreferencesConstants.TIME_ZONE, "Local Time");
        TmfTimestampFormat.updateDefaultFormats();
        super.after();
    }

    // ------------------------------------------------------------------------
    // Test case(s)
    // ------------------------------------------------------------------------
    /**
     * UI test of sorting of processes in CFV based on column selection. To verify that the sorting
     * was executed correctly, the test will use bot.waitUntil() the column content has the right
     * order.
     */
    @Test
    public void testColumnSorting() {
        // Create a known state
        applyFilter();
        final SWTBotTree tree = fViewBot.bot().tree();
        SWTBotTreeItem item = SWTBotUtils.getTreeItem(fBot, tree, SYSTEMD_PROCESS_NAME);
        item.select();

        testProcessSorting(tree);
        testTidSorting(tree);
        testPidSorting(tree);
        testBirthtimeSorting(tree);
        testTraceSorting(tree);
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------
    private void applyFilter() {
        // Select only certain root nodes and their children but filter out rest
        SWTBotToolbarButton filterButton = fViewBot.toolbarButton(FILTER_ACTION);
        filterButton.click();
        SWTBotShell shell = fBot.shell(FILTER_DIALOG_TITLE).activate();

        SWTBot bot = shell.bot();
        SWTBotTree treeBot = bot.tree();
        bot.button(UNCHECK_ALL).click();

        TreeCheckedCounter treeCheckCounter = new TreeCheckedCounter(treeBot);
        Integer checked = UIThreadRunnable.syncExec(treeCheckCounter);
        assertEquals("default", 0, checked.intValue());

        // select root nodes and their children
        checkTreeItem(bot, treeBot, SYSTEMD_PROCESS_NAME, 54);
        checkTreeItem(bot, treeBot, KTHREAD_PROCESS_NAME, 88);
        checkTreeItem(bot, treeBot, LTTNG_CONSUMER_PROCESS_NAME, 89);

        bot.button(OK_BUTTON).click();
    }

    private static void checkTreeItem(SWTBot bot, SWTBotTree treeBot, String process, int nbChecked) {
        SWTBotTreeItem item = SWTBotUtils.getTreeItem(bot, treeBot, process);
        item.select();
        bot.button(CHECK_SUBTREE).click();
        TreeCheckedCounter treeCheckCounter = new TreeCheckedCounter(treeBot);
        Integer checked = UIThreadRunnable.syncExec(treeCheckCounter);
        assertEquals(process, nbChecked, checked.intValue());
    }

    private static void testProcessSorting(final SWTBotTree tree) {
        SWTBotTreeColumn column = tree.header(PROCESS_COLUMN);
        String[] expected = { KTHREAD_PROCESS_NAME, LTTNG_CONSUMER_PROCESS_NAME, SYSTEMD_PROCESS_NAME };

        /* Sort direction Up */
        SWTBotTestCondition condition = getSortCondition(PROCESS_COLUMN, PROCESS_COLUMN_ID, expected, tree, false);
        clickColumn(tree, column, condition);

        /* Sort direction Down */
        condition = getSortCondition(PROCESS_COLUMN, 0, expected, tree, true);
        clickColumn(tree, column, condition);
    }

    private static void testTidSorting(final SWTBotTree tree) {
        String[] expected = { SYSTEMD_TID, KTHREAD_TID, LTTNG_CONSUMER_TID };
        SWTBotTreeColumn column = tree.header(TID_COLUMN);
        /* Sort direction Up */
        SWTBotTestCondition condition = getSortCondition(TID_COLUMN, TID_COLUMN_ID, expected, tree, false);
        clickColumn(tree, column, condition);

        /* Sort direction Down */
        condition = getSortCondition(TID_COLUMN, TID_COLUMN_ID, expected, tree, true);
        clickColumn(tree, column, condition);
    }

    /*
     * Note: In this test systemd and kthreadd have PTID 0 where
     * lttng-consumerd has PTID -1 that is unknown. Currently
     * in CFV PTID is only shown when it's greater than 0.
     */
    private static void testPidSorting(final SWTBotTree tree) {
        SWTBotTreeColumn column = tree.header(PTID_COLUMN);
        String[] expected = { LTTNG_CONSUMER_PROCESS_NAME, SYSTEMD_PROCESS_NAME, KTHREAD_PROCESS_NAME };
        /* Sort direction Up */
        SWTBotTestCondition condition = getSortCondition(PTID_COLUMN, PROCESS_COLUMN_ID, expected, tree, false);
        clickColumn(tree, column, condition);

        /* Sort direction Down */
        String[] expected2 = { SYSTEMD_PROCESS_NAME, KTHREAD_PROCESS_NAME, LTTNG_CONSUMER_PROCESS_NAME };
        condition = getSortCondition(PTID_COLUMN, PROCESS_COLUMN_ID, expected2, tree, false);
        clickColumn(tree, column, condition);
    }

    private static void testBirthtimeSorting(final SWTBotTree tree) {
        SWTBotTreeColumn column = tree.header(BIRTH_COLUMN);
        String[] expected = {
                Utils.formatTime(LTTNG_CONSUMER_BIRTHTIME, TimeFormat.CALENDAR, Resolution.NANOSEC),
                Utils.formatTime(SYSTEMD_BIRTHTIME, TimeFormat.CALENDAR, Resolution.NANOSEC),
                Utils.formatTime(KTHREAD_BIRTHTIME, TimeFormat.CALENDAR, Resolution.NANOSEC) };

        /* Sort direction Up */
        SWTBotTestCondition condition = getSortCondition(BIRTH_COLUMN, BIRTH_COLUMN_ID, expected, tree, false);
        clickColumn(tree, column, condition);

        /* Sort direction Down */
        condition = getSortCondition(BIRTH_COLUMN, BIRTH_COLUMN_ID, expected, tree, true);
        clickColumn(tree, column, condition);
    }

    /*
     * Note: In this test only one trace is visualized in CFV.
     * Sorting is done based on birth time (up direction).
     * The implementation in CFV won't change the order when changing
     * the direction.
     */
    private static void testTraceSorting(final SWTBotTree tree) {
        SWTBotTreeColumn column = tree.header(TRACE_COLUMN);
        String[] expected = { LTTNG_CONSUMER_PROCESS_NAME, SYSTEMD_PROCESS_NAME, KTHREAD_PROCESS_NAME };

        /* Sort direction Up */
        SWTBotTestCondition condition = getSortCondition(TRACE_COLUMN, PROCESS_COLUMN_ID, expected, tree, false);
        clickColumn(tree, column, condition);

        /* Sort direction Down */
        condition = getSortCondition(TRACE_COLUMN, PROCESS_COLUMN_ID, expected, tree, false);
        clickColumn(tree, column, condition);
    }

    private static void clickColumn(final SWTBotTree tree, SWTBotTreeColumn processColumn, SWTBotTestCondition condition) {
        processColumn.click();
        fBot.waitUntil(condition);
    }

    private static SWTBotTestCondition getSortCondition(final String testCase, final int cell, final String[] expected, final SWTBotTree tree, final boolean reverse) {
        return new SWTBotTestCondition() {
            @Override
            public boolean test() throws Exception {
                SWTBotTreeItem[] items = tree.getAllItems();
                if (reverse) {
                    for (int i = expected.length - 1; i > 0; i--) {
                        if (!expected[i].equals(items[expected.length - (i + 1)].cell(cell))) {
                            return false;
                        }
                    }
                } else {
                    for (int i = 0; i < expected.length; i++) {
                        if (!expected[i].equals(items[i].cell(cell))) {
                            return false;
                        }
                    }
                }
                return true;
            }
            @Override
            public String getFailureMessage() {
                return NLS.bind("Test Case: \'{0}\' failed!", testCase);
            }
        };
    }

}
