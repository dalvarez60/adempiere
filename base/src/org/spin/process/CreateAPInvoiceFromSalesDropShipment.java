/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it    		 *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope   		 *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 		 *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           		 *
 * See the GNU General Public License for more details.                       		 *
 * You should have received a copy of the GNU General Public License along    		 *
 * with this program; if not, write to the Free Software Foundation, Inc.,    		 *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     		 *
 * For the text or an alternative of this public license, you may reach us    		 *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com				  		                 *
 *************************************************************************************/
package org.spin.process;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MUOM;
import org.compiere.model.MUOMConversion;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * Generated Process for (Create AP Invoice From Sales)
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class CreateAPInvoiceFromSalesDropShipment extends CreateAPInvoiceFromSalesDropShipmentAbstract {
	
	/**	Counter for created	*/
	private AtomicInteger created = new AtomicInteger();
	/**	Lines	*/
	private Map<Integer, MInvoiceLine> invoiceLines = new HashMap<Integer, MInvoiceLine>();
	/**	Current Invoice	*/
	private MInvoice invoice;
	@Override
	protected void prepare() {
		super.prepare();
		if(getRecord_ID() == 0) {
			throw new AdempiereException("@C_Invoice_ID@ @NotFound@");
		}
		//	Get current invoice
		invoice = new MInvoice(getCtx(), getRecord_ID(), get_TrxName());
		if(invoice.isProcessed()) {
			throw new AdempiereException("@C_Invoice_ID@ @Processed@");
		}
	}

	@Override
	protected String doIt() throws Exception {
		//	Process selections
		getSelectionKeys().forEach(salesInvoiceLineId -> {
			int purchaseOrderLineId = getSelectionAsInt(salesInvoiceLineId, "POL_C_OrderLine_ID");
			MInvoiceLine salesInvoiceLine = new MInvoiceLine(getCtx(), salesInvoiceLineId, get_TrxName());
			MOrderLine salesOrderLine = (MOrderLine) salesInvoiceLine.getC_OrderLine();
			MOrderLine purchaseOrderLine = new MOrderLine (getCtx(), purchaseOrderLineId, get_TrxName());
			//	Set InOut
			String whereClause = "EXISTS (SELECT 1 "
					+ "FROM M_InOut io "
					+ "WHERE io.M_InOut_ID = M_InOutLine.M_InOut_ID "
					+ "AND io.DocStatus IN ('CO','CL'))";
			MInOutLine[] inOutLines = MInOutLine.getOfOrderLine(Env.getCtx(), purchaseOrderLineId, whereClause, get_TrxName());
			log.fine ("Receipt Lines with OrderLine = #" + inOutLines.length);
			MInOutLine inOutLine = Arrays.stream(inOutLines)
					.filter(ioLine -> ioLine != null && ioLine.getMovementQty().compareTo(salesInvoiceLine.getQtyInvoiced()) == 0)
					.findFirst().orElseGet(() -> inOutLines.length > 0 ? inOutLines[0] : null);
			//	Add lines
			MInvoiceLine invoiceLine = invoiceLines.get(purchaseOrderLineId);
			BigDecimal qtyEntered = Env.ZERO;
			BigDecimal qtyInvoiced = Env.ZERO;
			int uOMId = purchaseOrderLine.getC_UOM_ID();
			if(invoiceLine == null) {
				invoiceLine = new MInvoiceLine(invoice);
				//	Set From
				if(inOutLine != null) {
					invoiceLine.setShipLine(inOutLine);
				} else {
					invoiceLine.setOrderLine(purchaseOrderLine);
				}
				//	Set Quantity from Sales
				qtyInvoiced = salesInvoiceLine.getQtyInvoiced();
				invoiceLine.addDescription(Msg.parseTranslation(getCtx(), "@Created@ @from@ @C_Invoice_ID@ ") + salesInvoiceLine.getParent().getDocumentNo());
				invoiceLines.put(purchaseOrderLineId, invoiceLine);
			} else {
				//	Add Quantity from Sales
				qtyInvoiced = invoiceLine.getQtyInvoiced().add(salesInvoiceLine.getQtyInvoiced());
				invoiceLine.addDescription(salesInvoiceLine.getParent().getDocumentNo());
			}
			MProduct product = MProduct.get(Env.getCtx(), invoiceLine.getM_Product_ID());
			if(uOMId != product.getC_UOM_ID()) {
				MUOM orderUom = MUOM.get(getCtx(), uOMId);
				qtyEntered = MUOMConversion.convertProductFrom(getCtx(), product.getM_Product_ID(), uOMId, qtyInvoiced);
				if(qtyEntered == null) {
					qtyEntered = qtyInvoiced;
				}
				qtyEntered = qtyEntered.setScale(orderUom.getStdPrecision(), BigDecimal.ROUND_HALF_DOWN);
			} else {
				qtyEntered = qtyInvoiced;
			}
			invoiceLine.setQtyEntered(qtyEntered);
			invoiceLine.setQtyInvoiced(qtyInvoiced);
			invoiceLine.setC_UOM_ID(uOMId);
			//	Save
			invoiceLine.saveEx();
			//	Set reference
			salesOrderLine.setLink_OrderLine_ID(purchaseOrderLineId);
			salesOrderLine.addDescription(Msg.parseTranslation(getCtx(), "@POReference@ ") + purchaseOrderLine.getParent().getDocumentNo());
			salesOrderLine.saveEx();
			created.getAndIncrement();
		});
		return "@Created@ " + created;
	}
}