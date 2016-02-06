package org.eclipse.tracecompass.analysis.os.linux.ui.views.kernelmemoryusage;

import org.eclipse.osgi.util.NLS;

/**
 * @author Wassim Nasrallah
 * @author Mahdi Zolnouri
 *
 */
public class Messages {
    private static final String BUNDLE_NAME ="org.eclipse.tracecompass.analysis.os.linux.ui.views.kernelmemoryusage.messages";


    public static String KernelMemoryUsageComposite_ColumnProcess;
    public static String KernelMemoryUsageComposite_ColumnTID;
    public static String MemoryUsageView_title;
    public static String MemoryUsageViewer_title;
    public static String MemoryUsageViewer_xAxis;
    public static String MemoryUsageViewer_yAxis;
    public static String MemoryUsageViewer_Total;
    public static String SelectedThread_Value;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
