/*******************************************************************************
 * Copyright (c) 2013 hangum.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     hangum - initial API and implementation
 ******************************************************************************/
package com.hangum.tadpole.application.start.dialog.login;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.hangum.tadpole.application.start.Messages;
import com.hangum.tadpole.session.manager.SessionManager;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

/**
 * OTP Code Dialog
 * 
 * @author hangum
 *
 */
public class OTPLoginDialog extends Dialog {
	private int intOTPCode;
	private Text textOTPCode;
	

	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public OTPLoginDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	public void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.OTPLoginDialog_0);
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout) container.getLayout();
		gridLayout.verticalSpacing = 5;
		gridLayout.horizontalSpacing = 5;
		gridLayout.marginHeight = 5;
		gridLayout.marginWidth = 5;
		gridLayout.numColumns = 2;
		
		Label lblOtpCode = new Label(container, SWT.NONE);
		lblOtpCode.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblOtpCode.setText("OTP Code"); //$NON-NLS-1$
		
		textOTPCode = new Text(container, SWT.BORDER);
		textOTPCode.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		textOTPCode.setFocus();
		return container;
	}
	
	@Override
	protected void okPressed() {
		String strOTPCode = StringUtils.trim(textOTPCode.getText());
		if(!NumberUtils.isNumber(strOTPCode)) {
			textOTPCode.setFocus();
			MessageDialog.openError(getShell(), "Error", Messages.OTPLoginDialog_3); //$NON-NLS-1$
			return;
		}
		
		setIntOTPCode(NumberUtils.toInt(strOTPCode));
		
		super.okPressed();
	}
	
	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "OK", true); //$NON-NLS-1$
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 115);
	}

	public int getIntOTPCode() {
		return intOTPCode;
	}

	public void setIntOTPCode(int intOTPCode) {
		this.intOTPCode = intOTPCode;
	}

}