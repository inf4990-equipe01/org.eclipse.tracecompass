/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.analysis.timing.core.segmentstore;

import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;

/**
 * Analysis progress listener for segment store viewers
 *
 * @author France Lapointe Nguyen
 * @since 2.0
 */
public interface IAnalysisProgressListener {

    /**
     * Called at the end of the analysis
     *
     * @param activeAnalysis
     *            analysis that is running
     *
     * @param data
     *            segment store of the latency analysis
     */
    void onComplete(AbstractSegmentStoreAnalysisModule activeAnalysis, ISegmentStore<ISegment> data);

}
