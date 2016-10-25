/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Google Inc - add support for accepting multiple connections
 *******************************************************************************/

package com.google.cloud.tools.eclipse.jdi.internal.connect;

import com.sun.jdi.connect.Connector.IntegerArgument;

/**
 * A fork of {@link org.eclipse.jdi.internal.connect.IntegerArgumentImpl},
 * required as the original has package protection.
 */
@SuppressWarnings({"restriction","serial"})
public class _IntegerArgumentImpl implements IntegerArgument {
	private String fLabel;
	private String fDescription;
	private int fValue;
	private int fMinimum;
	private int fMaximum;
	private String fName;
	private boolean fMustSpecify;

	public _IntegerArgumentImpl(String name, String description, String label, boolean mustSpecify, int min, int max) {
		fName = name;
		fDescription = description;
		fLabel = label;
		fMustSpecify = mustSpecify;
		fMinimum = min;
		fMaximum = max;
	}

	@Override
	public String name() {
		return fName;
	}

	@Override
	public String description() {
		return fDescription;
	}

	@Override
	public String label() {
		return fLabel;
	}

	@Override
	public boolean mustSpecify() {
		return fMustSpecify;
	}

	@Override
	public void setValue(String value) {
		fValue = Integer.parseInt(value);
	}

	@Override
	public String value() {
		return Integer.toString(fValue);
	}

	@Override
	public int intValue() {
		return fValue;
	}

	@Override
	public boolean isValid(int v) {
		return fMinimum <= v && v <= fMaximum;
	}

	@Override
	public boolean isValid(String v) {
		return isValid(Integer.parseInt(v));
	}

	@Override
	public int max() {
		return fMaximum;
	}

	@Override
	public int min() {
		return fMinimum;
	}

	@Override
	public void setValue(int v) {
		fValue = v;
	}

	@Override
	public String stringValueOf(int v) {
		return Integer.toString(v);
	}
}
