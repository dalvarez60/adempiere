/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/

package org.adempiere.pos.process;

import org.compiere.process.SvrProcess;

/** Generated Process for (Reverse The Sales Transaction)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public abstract class ReverseTheSalesTransactionAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "C_POS ReverseTheSalesTransaction";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Reverse The Sales Transaction";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 53824;
	/**	Parameter Name for Order	*/
	public static final String C_ORDER_ID = "C_Order_ID";
	/**	Parameter Name for Invoice Partner	*/
	public static final String BILL_BPARTNER_ID = "Bill_BPartner_ID";
	/**	Parameter Name for Cancelled	*/
	public static final String ISCANCELLED = "IsCancelled";
	/**	Parameter Name for Ship/Receipt Confirmation	*/
	public static final String ISSHIPCONFIRM = "IsShipConfirm";
	/**	Parameter Name for Document Type for Return Order	*/
	public static final String C_DOCTYPERMA_ID = "C_DocTypeRMA_ID";
	/**	Parameter Value for Order	*/
	private int orderId;
	/**	Parameter Value for Invoice Partner	*/
	private int bPartnerId;
	/**	Parameter Value for Cancelled	*/
	private boolean isCancelled;
	/**	Parameter Value for Ship/Receipt Confirmation	*/
	private boolean isShipConfirm;
	/**	Parameter Value for Document Type for Return Order	*/
	private int docTypeRMAId;

	@Override
	protected void prepare() {
		orderId = getParameterAsInt(C_ORDER_ID);
		bPartnerId = getParameterAsInt(BILL_BPARTNER_ID);
		isCancelled = getParameterAsBoolean(ISCANCELLED);
		isShipConfirm = getParameterAsBoolean(ISSHIPCONFIRM);
		docTypeRMAId = getParameterAsInt(C_DOCTYPERMA_ID);
	}

	/**	 Getter Parameter Value for Order	*/
	protected int getOrderId() {
		return orderId;
	}

	/**	 Setter Parameter Value for Order	*/
	protected void setOrderId(int orderId) {
		this.orderId = orderId;
	}

	/**	 Getter Parameter Value for Invoice Partner	*/
	protected int getBPartnerId() {
		return bPartnerId;
	}

	/**	 Setter Parameter Value for Invoice Partner	*/
	protected void setBPartnerId(int bPartnerId) {
		this.bPartnerId = bPartnerId;
	}

	/**	 Getter Parameter Value for Cancelled	*/
	protected boolean isCancelled() {
		return isCancelled;
	}

	/**	 Setter Parameter Value for Cancelled	*/
	protected void setIsCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}

	/**	 Getter Parameter Value for Ship/Receipt Confirmation	*/
	protected boolean isShipConfirm() {
		return isShipConfirm;
	}

	/**	 Setter Parameter Value for Ship/Receipt Confirmation	*/
	protected void setIsShipConfirm(boolean isShipConfirm) {
		this.isShipConfirm = isShipConfirm;
	}

	/**	 Getter Parameter Value for Document Type for Return Order	*/
	protected int getDocTypeRMAId() {
		return docTypeRMAId;
	}

	/**	 Setter Parameter Value for Document Type for Return Order	*/
	protected void setDocTypeRMAId(int docTypeRMAId) {
		this.docTypeRMAId = docTypeRMAId;
	}

	/**	 Getter Parameter Value for Process ID	*/
	public static final int getProcessId() {
		return ID_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Value	*/
	public static final String getProcessValue() {
		return VALUE_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Name	*/
	public static final String getProcessName() {
		return NAME_FOR_PROCESS;
	}
}