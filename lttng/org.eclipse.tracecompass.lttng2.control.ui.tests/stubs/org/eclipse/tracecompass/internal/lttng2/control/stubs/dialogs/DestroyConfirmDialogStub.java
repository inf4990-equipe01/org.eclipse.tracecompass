/**********************************************************************
 * Copyright (c) 2012, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.tracecompass.internal.lttng2.control.stubs.dialogs;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.dialogs.IConfirmDialog;

/**
 * A confirmation dialog stub implementation.
 */
public class DestroyConfirmDialogStub implements IConfirmDialog {

    @Override
    public boolean openConfirm(Shell parent, String title, String message) {
        return true;
    }
}