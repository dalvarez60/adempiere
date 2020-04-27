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

/** Generated Process for (Create Order based on another)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public abstract class CreateOrderBasedOnAnotherAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "C_POS CreateOrderBasedOnAnother";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Create Order based on another";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 53822;
	/**	Parameter Name for Order Source	*/
	public static final String C_ORDERSOURCE_ID = "C_OrderSource_ID";
	/**	Parameter Name for Invoice Partner	*/
	public static final String BILL_BPARTNER_ID = "Bill_BPartner_ID";
	/**	Parameter Name for SO Sub Type	*/
	public static final String DOCSUBTYPESO = "DocSubTypeSO";
	/**	Parameter Name for Document Action	*/
	public static final String DOCACTION = "DocAction";
	/**	Parameter Name for Include Payments	*/
	public static final String ISINCLUDEPAYMENTS = "IsIncludePayments";
	/**	Parameter Name for Allocated	*/
	public static final String ISALLOCATED = "IsAllocated";
	/**	Parameter Name for Document Type for Return Order	*/
	public static final String C_DOCTYPERMA_ID = "C_DocTypeRMA_ID";
	/**	Parameter Value for Order Source	*/
	private int orderSourceId;
	/**	Parameter Value for Invoice Partner	*/
	private int bPartnerId;
	/**	Parameter Value for SO Sub Type	*/
	private String docSubTypeSO;
	/**	Parameter Value for Document Action	*/
	private String docAction;
	/**	Parameter Value for Include Payments	*/
	private String isIncludePayments;
	/**	Parameter Value for Allocated	*/
	private String isAllocated;
	/**	Parameter Value for Document Type for Return Order	*/
	private int docTypeRMAId;

	@Override
	protected void prepare() {
		orderSourceId = getParameterAsInt(C_ORDERSOURCE_ID);
		bPartnerId = getParameterAsInt(BILL_BPARTNER_ID);
		docSubTypeSO = getParameterAsString(DOCSUBTYPESO);
		docAction = getParameterAsString(DOCACTION);
		isIncludePayments = getParameterAsString(ISINCLUDEPAYMENTS);
		isAllocated = getParameterAsString(ISALLOCATED);
		docTypeRMAId = getParameterAsInt(C_DOCTYPERMA_ID);
	}

	/**	 Getter Parameter Value for Order Source	*/
	protected int getOrderSourceId() {
		return orderSourceId;
	}

	/**	 Setter Parameter Value for Order Source	*/
	protected void setOrderSourceId(int orderSourceId) {
		this.orderSourceId = orderSourceId;
	}

	/**	 Getter Parameter Value for Invoice Partner	*/
	protected int getBPartnerId() {
		return bPartnerId;
	}

	/**	 Setter Parameter Value for Invoice Partner	*/
	protected void setBPartnerId(int bPartnerId) {
		this.bPartnerId = bPartnerId;
	}

	/**	 Getter Parameter Value for SO Sub Type	*/
	protected String getDocSubTypeSO() {
		return docSubTypeSO;
	}

	/**	 Setter Parameter Value for SO Sub Type	*/
	protected void setDocSubTypeSO(String docSubTypeSO) {
		this.docSubTypeSO = docSubTypeSO;
	}

	/**	 Getter Parameter Value for Document Action	*/
	protected String getDocAction() {
		return docAction;
	}

	/**	 Setter Parameter Value for Document Action	*/
	protected void setDocAction(String docAction) {
		this.docAction = docAction;
	}

	/**	 Getter Parameter Value for Include Payments	*/
	protected String getIsIncludePayments() {
		return isIncludePayments;
	}

	/**	 Setter Parameter Value for Include Payments	*/
	protected void setIsIncludePayments(String isIncludePayments) {
		this.isIncludePayments = isIncludePayments;
	}

	/**	 Getter Parameter Value for Allocated	*/
	protected String getIsAllocated() {
		return isAllocated;
	}

	/**	 Setter Parameter Value for Allocated	*/
	protected void setIsAllocated(String isAllocated) {
		this.isAllocated = isAllocated;
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