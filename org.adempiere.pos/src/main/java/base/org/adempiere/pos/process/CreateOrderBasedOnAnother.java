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

import org.compiere.model.I_C_Order;
import org.compiere.model.I_M_InOut;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MPayment;
import org.compiere.model.PO;
import org.compiere.process.DocAction;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.eevolution.service.dsl.ProcessBuilder;
import org.spin.process.OrderRMACreateFrom;
import org.spin.process.OrderRMACreateFromInvoice;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * This process allows create a new sales order based on other and change the business partner
 * all payments and allocations can be replicated for new order with new business partner
 * eEvolution author Victor Perez <victor.perez@e-evolution.com>, Created by e-Evolution on 23/12/15.
 * @contributor Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 * 		<a href="https://github.com/adempiere/adempiere/issues/670">
 * 		@see FR [ 670 ] Standard process for return material on POS</a>
 */
public class CreateOrderBasedOnAnother extends CreateOrderBasedOnAnotherAbstract {
	
    private Timestamp today;


    @Override
    protected void prepare() {
        super.prepare();
    }

    @Override
    protected String doIt() throws Exception {
    	today = new Timestamp(System.currentTimeMillis());
        // Get Order
        MOrder sourceOrder = new MOrder(getCtx(), getOrderSourceId(), get_TrxName());
        //Create new Order based on source order
        MOrder returnOrder = null;
        //	Get Invoices for ths order
        List<MInOut> shipments = Arrays.asList(sourceOrder.getShipments());
        // If not exist invoice then only is necessary reverse shipment
        if (shipments.size() > 0) {
        	// Validate if partner not is POS partner standard then reverse shipment
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
        } else {
        	List<MInvoice> invoices = Arrays.asList(sourceOrder.getInvoices());
        	if(invoices.size() > 0) {
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
        addLog(returnOrder.getDocumentNo());
        //	Set Record ID
        getProcessInfo().setRecord_ID(returnOrder.get_ID());
        String message = "@C_Order_ID@ " + returnOrder.getDocumentNo();
        //	Validate Document Action
        if(!returnOrder.isProcessed()) {
        	return message;
        }

        if(getIsIncludePayments() != null
        		&& getIsIncludePayments().equals("Y"))
            createPayments(sourceOrder, returnOrder);
        if (getIsAllocated() != null
        		&& getIsAllocated().equals("Y"))
            createAllocations(returnOrder);
        //	Default Return
        return message;
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

    /**
     * Create Allocations for new order
     * @param targetOrder
     */
    private void createAllocations(MOrder targetOrder) {
        List<MPayment> payments = MPayment.getOfOrder(targetOrder);
        MInvoice[] invoices = targetOrder.getInvoices();
        BigDecimal totalPay = BigDecimal.ZERO;
        BigDecimal totalInvoiced =  BigDecimal.ZERO;
        for (MPayment payment : payments)
            totalPay = totalPay.add(payment.getPayAmt());

        for (MInvoice invoice : invoices)
        {
            totalInvoiced =  totalInvoiced.add(invoice.getGrandTotal());
        }

        if (totalInvoiced.signum() != 0
         && totalPay.signum() != 0
         && totalInvoiced.compareTo(totalPay) == 0)
        {
            MAllocationHdr allocation = new MAllocationHdr(
                    getCtx() ,
                    true ,
                    today ,
                    targetOrder.getC_Currency_ID() ,
                    targetOrder.getDescription() ,
                    get_TrxName());
            allocation.setDocStatus(org.compiere.process.DocAction.STATUS_Drafted);
            allocation.setDocAction(org.compiere.process.DocAction.ACTION_Complete);
            allocation.saveEx();
            addLog(allocation.getDocumentInfo());
            for (MInvoice invoice : invoices)
            {
                MAllocationLine allocationLine =  new MAllocationLine(allocation);
                allocationLine.setDocInfo(targetOrder.getC_BPartner_ID() , targetOrder.getC_Order_ID() , invoice.getC_Invoice_ID());
                allocationLine.setAmount(invoice.getGrandTotal());
                allocationLine.saveEx();
            }

            for (MPayment payment : payments)
            {
                MAllocationLine allocationLine = new MAllocationLine(allocation);
                allocationLine.setPaymentInfo(payment.get_ID() , 0 );
                allocationLine.setAmount(payment.getPayAmt());
                allocationLine.saveEx();
            }

            allocation.processIt(org.compiere.process.DocAction.ACTION_Complete);
            allocation.saveEx();
        }
    }

    /**
     * Create payment for new Order
     * @param sourceOrder
     * @param targetOrder
     */
    private void createPayments(MOrder sourceOrder , MOrder targetOrder) {
        for (MPayment sourcePayment : MPayment.getOfOrder(sourceOrder)) {
            MPayment payment = new MPayment(getCtx() ,  0 , get_TrxName());
            PO.copyValues(sourcePayment, payment);
            payment.setDateTrx(today);
            payment.setDateAcct(today);
            payment.setC_Order_ID(targetOrder.getC_Order_ID());
            payment.setC_BPartner_ID(targetOrder.getC_BPartner_ID());
            payment.setC_Invoice_ID(-1);
            payment.addDescription(Msg.parseTranslation(sourceOrder.getCtx() , " @From@ ") + sourcePayment.getDocumentNo());
            payment.setIsReceipt(true);
            payment.setC_DocType_ID(MDocType.getDocType(MDocType.DOCBASETYPE_ARReceipt, sourceOrder.getAD_Org_ID()));
            payment.setIsPrepayment(true);
            payment.saveEx();

            payment.processIt(getDocAction());
            payment.saveEx();
            addLog(payment.getDocumentInfo());
        }
    }
}
