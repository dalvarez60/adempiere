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

import org.compiere.model.I_C_Order;
import org.compiere.model.I_M_InOut;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MOrder;
import org.compiere.model.MPayment;
import org.compiere.model.PO;
import org.compiere.process.DocAction;
import org.compiere.process.InOutGenerate;
import org.compiere.process.InvoiceGenerate;
import org.compiere.process.ProcessInfo;
import org.eevolution.service.dsl.ProcessBuilder;
import org.spin.process.OrderRMACreateFrom;


/**
 * Process allows reverse the sales order using new documents with new dates and cancel of original effects
 * eEvolution author Victor Perez <victor.perez@e-evolution.com>, Created by e-Evolution on 23/12/15.
 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/adempiere/issues/1062">
 * 		@see FR [ 1062 ] Throw exception on Reverse Sales Transaction</a>
 */
public class ReverseTheSalesTransaction extends ReverseTheSalesTransactionAbstract  {
    private Timestamp today;
    private List<MInOut> customerReturns = new ArrayList<MInOut>();


    @Override
    protected void prepare() {
        super.prepare();
    }

    @Override
    protected String doIt() throws Exception {
        today = new Timestamp(System.currentTimeMillis());
        // Get Order
        MOrder sourceOrder = new MOrder(getCtx(), getOrderId(), get_TrxName());
        ProcessInfo processInformation = ProcessBuilder
        	.create(getCtx())
        	.process(CreateOrderBasedOnAnother.getProcessId())
        	.withTitle(CreateOrderBasedOnAnother.getProcessName())
        	.withParameter(CreateOrderBasedOnAnother.C_OrderSource_ID, getOrderId())
        	.withParameter(CreateOrderBasedOnAnother.Bill_BPartner_ID, sourceOrder.getC_BPartner_ID())
        	.withParameter(CreateOrderBasedOnAnother.DocSubTypeSO , MDocType.DOCSUBTYPESO_ReturnMaterial)
        	.withParameter(CreateOrderBasedOnAnother.DocAction, DocAction.ACTION_None)
        	.withParameter(CreateOrderBasedOnAnother.IsIncludePayments, false)
        	.withParameter(CreateOrderBasedOnAnother.IsAllocated, false)
        	.withoutTransactionClose()
        	.execute(get_TrxName());
        //	
        if(processInformation.getRecord_ID() != 0) {
        	MOrder returnOrder = new MOrder(getCtx(), processInformation.getRecord_ID(), get_TrxName());
            // Get Invoices for ths order
            List<MInOut> shipments = Arrays.asList(sourceOrder.getShipments());
            // If not exist invoice then only is necessary reverse shipment
            if (shipments.size() > 0) {
                // Validate if partner not is POS partner standard then reverse shipment
                if (sourceOrder.getC_BPartner_ID() != getInvoicePartnerId() || isCancelled()) {
                    List<Integer> selectedRecordsIds = new ArrayList<>();
                	ProcessBuilder builder = ProcessBuilder
                    	.create(getCtx())
                    	.process(OrderRMACreateFrom.getProcessId())
                    	.withRecordId(I_C_Order.Table_ID, returnOrder.getC_Order_ID());
                	//	
                	LinkedHashMap<Integer, LinkedHashMap<String, Object>> selection = new LinkedHashMap<>();
                	shipments.forEach(sourceShipment -> {
                		LinkedHashMap<String, Object> selectionValues = new LinkedHashMap<String, Object>();
                		//	Add values
                		Arrays.asList(sourceShipment.getLines())
                			.forEach(sourceShipmentLine -> {
                				selectionValues.put("CF_M_Product_ID", sourceShipmentLine.getM_Product_ID());
                	    		selectionValues.put("CF_C_Charge_ID", sourceShipmentLine.getC_Charge_ID());
                	    		selectionValues.put("CF_C_UOM_ID", sourceShipmentLine.getC_UOM_ID());
                			});
                		selection.put(sourceShipment.getM_InOut_ID(), selectionValues);
                	});
                	//	
                	builder.withSelectedRecordsIds(I_M_InOut.Table_ID, selectedRecordsIds, selection)
                		.withoutTransactionClose()
                		.execute(get_TrxName());
                }
            }
            //	Process return Order
            if(!returnOrder.processIt(DocAction.ACTION_Complete)) {
            	return returnOrder.getProcessMsg();
            }
            //	Save if is ok
            returnOrder.saveEx();
            //	Generate Invoice
            if (sourceOrder.getC_BPartner_ID() != getInvoicePartnerId() || isCancelled()) {
            	ProcessBuilder
                    	.create(getCtx())
                    	.process(InvoiceGenerate.getProcessId())
                    	.withTitle(InvoiceGenerate.getProcessName())
                    	.withParameter(InvoiceGenerate.AD_ORG_ID, sourceOrder.getAD_Org_ID())
                    	.withParameter(InvoiceGenerate.C_ORDER_ID, returnOrder.getC_Order_ID())
                    	.withoutTransactionClose()
                    	.execute(get_TrxName());
            }
            //	Generate Return
            if (sourceOrder.getC_BPartner_ID() != getInvoicePartnerId() || isCancelled()) {
            	ProcessBuilder
                    	.create(getCtx())
                    	.process(InOutGenerate.getProcessId())
                    	.withTitle(InOutGenerate.getProcessName())
                    	.withParameter(InOutGenerate.M_WAREHOUSE_ID, sourceOrder.getM_Warehouse_ID())
                    	.withSelectedRecordsIds(I_C_Order.Table_ID, Arrays.asList(returnOrder.getC_Order_ID()))
                    	.withoutTransactionClose()
                    	.execute(get_TrxName());
            }
        }
        //Cancel original payment
        for (MPayment payment :cancelPayments(sourceOrder, today)) {
        	addLog(payment.getDocumentInfo());
        }
        sourceOrder.processIt(DocAction.ACTION_Close);
        sourceOrder.saveEx();
        return "@Ok@";
    }

    /**
     * Cancel Payments
     * @param sourceOrder
     * @param today
     * @return
     */
    private List<MPayment> cancelPayments(MOrder sourceOrder, Timestamp today) {
        List<MPayment> payments = new ArrayList<>();
        List<MPayment> sourcePayments = MPayment.getOfOrder(sourceOrder);
        for (MPayment sourcePayment : sourcePayments)
        {
            MPayment payment = new MPayment(sourceOrder.getCtx() ,  0 , sourceOrder.get_TrxName());
            PO.copyValues(sourcePayment, payment);
            payment.setDateTrx(today);
            payment.setC_Order_ID(sourceOrder.getC_Order_ID());
            payment.setDateAcct(today);
            payment.addDescription(" @From@ " + sourcePayment.getDocumentNo());
            payment.setIsReceipt(false);
            payment.setC_DocType_ID(MDocType.getDocType(MDocType.DOCBASETYPE_APPayment));
            payment.setDocAction(DocAction.ACTION_Complete);
            payment.setDocStatus(DocAction.STATUS_Drafted);
            payment.setIsPrepayment(true);
            payment.saveEx();

            payment.processIt(DocAction.ACTION_Complete);
            payment.saveEx();
            payments.add(payment);
        }
        return payments;
    }
}
