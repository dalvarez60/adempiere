/** ****************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2016 e-Evolution,SC. All Rights Reserved.               *
 * Contributor(s): Victor Perez www.e-evolution.com                           *
 * ****************************************************************************/

package org.adempiere.pos.process;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_Order;
import org.compiere.model.I_M_InOut;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MPayment;
import org.compiere.model.PO;
import org.compiere.process.DocAction;
import org.compiere.process.InOutGenerate;
import org.compiere.process.InvoiceGenerate;
import org.compiere.process.ProcessInfo;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.eevolution.service.dsl.ProcessBuilder;
import org.spin.process.OrderRMACreateFrom;
import org.spin.process.OrderRMACreateFromInvoice;


/**
 * Process allows reverse the sales order using new documents with new dates and cancel of original effects
 * eEvolution author Victor Perez <victor.perez@e-evolution.com>, Created by e-Evolution on 23/12/15.
 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/adempiere/issues/1062">
 * 		@see FR [ 1062 ] Throw exception on Reverse Sales Transaction</a>
 */
public class ReverseTheSalesTransaction extends ReverseTheSalesTransactionAbstract  {
    private Timestamp today;
    @Override
    protected void prepare() {
        super.prepare();
    }

    @Override
    protected String doIt() throws Exception {
        today = new Timestamp(System.currentTimeMillis());
        // Get Order
        MOrder sourceOrder = new MOrder(getCtx(), getOrderId(), get_TrxName());
        //Create new Order based on source order
        MOrder returnOrder = null;
        //	Get Invoices for ths order
        List<MInOut> shipments = Arrays.asList(sourceOrder.getShipments());
        boolean isDelivered = false;
        // If not exist invoice then only is necessary reverse shipment
        if (shipments.size() > 0) {
        	isDelivered = true;
        	// Validate if partner not is POS partner standard then reverse shipment
            if (sourceOrder.getC_BPartner_ID() != getBPartnerId() || isCancelled()) {
            	returnOrder = createReturnSource(sourceOrder);
                List<Integer> selectedRecordsIds = new ArrayList<>();
            	ProcessBuilder builder = ProcessBuilder
                	.create(getCtx())
                	.process(OrderRMACreateFrom.getProcessId())
                	.withRecordId(I_C_Order.Table_ID, returnOrder.getC_Order_ID());
            	//	
            	LinkedHashMap<Integer, LinkedHashMap<String, Object>> selection = new LinkedHashMap<>();
            	shipments.forEach(sourceShipment -> {
            		//	Add values
            		Arrays.asList(sourceShipment.getLines())
            			.forEach(sourceShipmentLine -> {
            				LinkedHashMap<String, Object> selectionValues = new LinkedHashMap<String, Object>();
            				selectionValues.put("CF_M_Product_ID", sourceShipmentLine.getM_Product_ID());
            	    		selectionValues.put("CF_C_Charge_ID", sourceShipmentLine.getC_Charge_ID());
            	    		selectionValues.put("CF_C_UOM_ID", sourceShipmentLine.getC_UOM_ID());
            	    		selectionValues.put("CF_QtyEntered", sourceShipmentLine.getQtyEntered());
            	    		selectedRecordsIds.add(sourceShipmentLine.getM_InOutLine_ID());
            	    		selection.put(sourceShipmentLine.getM_InOutLine_ID(), selectionValues);
            			});
            	});
            	//	
            	builder.withSelectedRecordsIds(I_M_InOut.Table_ID, selectedRecordsIds, selection)
            		.withoutTransactionClose()
            		.execute(get_TrxName());
            }
        } else {
        	List<MInvoice> invoices = Arrays.asList(sourceOrder.getInvoices());
        	if(invoices.size() > 0) {
        		if (sourceOrder.getC_BPartner_ID() != getBPartnerId() || isCancelled()) {
            		returnOrder = createReturnSource(sourceOrder);
                    List<Integer> selectedRecordsIds = new ArrayList<>();
                	ProcessBuilder builder = ProcessBuilder
                    	.create(getCtx())
                    	.process(OrderRMACreateFromInvoice.getProcessId())
                    	.withRecordId(I_C_Order.Table_ID, returnOrder.getC_Order_ID());
                	//	
                	LinkedHashMap<Integer, LinkedHashMap<String, Object>> selection = new LinkedHashMap<>();
                	invoices.forEach(invoice -> {
                		//	Add values
                		Arrays.asList(invoice.getLines())
                			.forEach(sourceInvoiceLine -> {
                				LinkedHashMap<String, Object> selectionValues = new LinkedHashMap<String, Object>();
                				selectionValues.put("CF_M_Product_ID", sourceInvoiceLine.getM_Product_ID());
                	    		selectionValues.put("CF_C_Charge_ID", sourceInvoiceLine.getC_Charge_ID());
                	    		selectionValues.put("CF_C_UOM_ID", sourceInvoiceLine.getC_UOM_ID());
                	    		selectionValues.put("CF_QtyEntered", sourceInvoiceLine.getQtyEntered());
                	    		selectedRecordsIds.add(sourceInvoiceLine.getC_InvoiceLine_ID());
                	    		selection.put(sourceInvoiceLine.getC_InvoiceLine_ID(), selectionValues);
                			});
                	});
                	//	
                	builder.withSelectedRecordsIds(I_M_InOut.Table_ID, selectedRecordsIds, selection)
                		.withoutTransactionClose()
                		.execute(get_TrxName());
            	}
        	}
        }
        //	Process return Order
        if(!returnOrder.processIt(DocAction.ACTION_Complete)) {
        	return returnOrder.getProcessMsg();
        }
        //	Save if is ok
        returnOrder.saveEx();
        //	Set Record ID
        getProcessInfo().setRecord_ID(returnOrder.get_ID());
        //	Generate Return
        if(isDelivered) {
        	if (sourceOrder.getC_BPartner_ID() != getBPartnerId() || isCancelled()) {
            	ProcessBuilder
                    	.create(getCtx())
                    	.process(InOutGenerate.getProcessId())
                    	.withTitle(InOutGenerate.getProcessName())
                    	.withParameter(InOutGenerate.M_WAREHOUSE_ID, sourceOrder.getM_Warehouse_ID())
                    	.withParameter(InOutGenerate.DOCACTION, DocAction.ACTION_Complete)
                    	.withSelectedRecordsIds(I_C_Order.Table_ID, Arrays.asList(returnOrder.getC_Order_ID()))
                    	.withoutTransactionClose()
                    	.execute(get_TrxName());
            }
        }
        //	Generate Invoice
        ProcessInfo invoiceInformation = null;
        if (sourceOrder.getC_BPartner_ID() != getBPartnerId() || isCancelled()) {
        	invoiceInformation = ProcessBuilder
                	.create(getCtx())
                	.process(InvoiceGenerate.getProcessId())
                	.withTitle(InvoiceGenerate.getProcessName())
                	.withParameter(InvoiceGenerate.AD_ORG_ID, sourceOrder.getAD_Org_ID())
                	.withParameter(InvoiceGenerate.C_ORDER_ID, returnOrder.getC_Order_ID())
                	.withParameter(InvoiceGenerate.DOCACTION, DocAction.ACTION_Complete)
                	.withoutTransactionClose()
                	.execute(get_TrxName());
        }
        //	Validate Credit Memo
        if(invoiceInformation == null
        		|| invoiceInformation.getRecord_ID() == 0) {
        	throw new AdempiereException("@C_Invoice_ID@ @NotFound@");
        }
        //Cancel original payment
        cancelPayments(sourceOrder, new MInvoice(getCtx(), invoiceInformation.getRecord_ID(), get_TrxName()))
        	.forEach(payment -> addLog(payment.getDocumentInfo()));
        sourceOrder.processIt(DocAction.ACTION_Close);
        sourceOrder.saveEx();
        if(isDelivered) {
        	returnOrder.processIt(DocAction.ACTION_Close);
        }
        return "@Ok@";
    }

    /**
     * Cancel Payments
     * @param creditMemo
     * @return
     */
    private List<MPayment> cancelPayments(MOrder sourceOrder, MInvoice creditMemo) {
        List<MPayment> payments = new ArrayList<>();
        MPayment.getOfOrder(sourceOrder).forEach(sourcePayment -> {
        	MPayment payment = new MPayment(getCtx() ,  0 , get_TrxName());
            PO.copyValues(sourcePayment, payment);
            payment.setDateTrx(today);
            payment.setDateAcct(today);
            payment.addDescription(Msg.parseTranslation(getCtx(), " @From@ " + sourcePayment.getDocumentNo() + " @of@ @C_Order_ID@ " + sourceOrder.getDocumentNo()));
            payment.setIsReceipt(false);
            payment.setC_DocType_ID(MDocType.getDocType(MDocType.DOCBASETYPE_APPayment));
            payment.setDocAction(DocAction.ACTION_Complete);
            payment.setDocStatus(DocAction.STATUS_Drafted);
            payment.setIsPrepayment(true);
            payment.setC_Order_ID(creditMemo.getC_Order_ID());
            payment.setC_Invoice_ID(creditMemo.getC_Invoice_ID());
            payment.saveEx();
            payment.processIt(DocAction.ACTION_Complete);
            payment.saveEx();
            payments.add(payment);
        });
        return payments;
    }
    
    /**
     * Create Return Order
     * @param source
     * @return
     */
    private MOrder createReturnSource(MOrder source) {
    	MOrder target = new MOrder (getCtx(), 0, get_TrxName());
		target.set_TrxName(get_TrxName());
		PO.copyValues(source, target, false);
		//
		target.setDocStatus (DocAction.STATUS_Drafted);		//	Draft
		target.setDocAction(DocAction.ACTION_Complete);
		//
		target.setIsSelected (false);
		target.setDateOrdered(today);
		target.setDateAcct(today);
		target.setDatePromised(today);	//	assumption
		target.setDatePrinted(null);
		target.setIsPrinted (false);
		//
		target.setIsApproved (false);
		target.setIsCreditApproved(false);
		target.setC_Payment_ID(0);
		target.setC_CashLine_ID(0);
		//	Amounts are updated  when adding lines
		target.setGrandTotal(Env.ZERO);
		target.setTotalLines(Env.ZERO);
		//
		target.setIsDelivered(false);
		target.setIsInvoiced(false);
		target.setIsSelfService(false);
		target.setIsTransferred (false);
		target.setPosted (false);
		target.setProcessed (false);
		target.save(source.get_TrxName());
		//	Set Document base for return
		if(getDocTypeRMAId() != 0) {
        	target.setC_DocTypeTarget_ID(getDocTypeRMAId());
        } else {
        	target.setC_DocTypeTarget_ID(MDocType.getDocTypeBaseOnSubType(source.getAD_Org_ID(), 
            		MDocType.DOCBASETYPE_SalesOrder , MDocType.DOCSUBTYPESO_ReturnMaterial));
    	}
        //	Set references
		target.setC_BPartner_ID(getBPartnerId());
		target.setRef_Order_ID(source.getC_Order_ID());
		target.setProcessed(false);
		target.saveEx();
		return target;
    }
}
