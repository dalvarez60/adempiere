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

package org.adempiere.pos.command;

import java.util.Arrays;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_Order;
import org.compiere.model.MBankStatement;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MPOS;
import org.compiere.model.MPayment;
import org.compiere.process.DocAction;
import org.compiere.process.InOutGenerate;
import org.compiere.process.InvoiceGenerate;
import org.compiere.process.ProcessInfo;
import org.compiere.util.Trx;
import org.compiere.util.TrxRunnable;
import org.compiere.util.Util;
import org.eevolution.service.dsl.ProcessBuilder;

/**
 * execute Complete document command
 * eEvolution author Victor Perez <victor.perez@e-evolution.com>, Created by e-Evolution on 23/01/16.
 */
public class CommandCompleteDocument extends CommandAbstract implements Command {
    public CommandCompleteDocument(String command, String event) {

        super.command = command;
        super.event = event;
    }

    @Override
    public void execute(CommandReceiver commandReceiver) {
        Trx.run(new TrxRunnable() {
            public void run(String trxName) {
                //Create partial return
                MOrder order = new MOrder(commandReceiver.getCtx(), commandReceiver.getOrderId(), trxName);
                order.setDocAction(DocAction.ACTION_Complete);
                order.processIt(DocAction.ACTION_Complete);
                order.saveEx();
                ProcessInfo processInformation = new ProcessInfo("Complete Order", 0, I_C_Order.Table_ID, order.getC_Order_ID());
                processInformation.setSummary("@C_Order_ID@: " + order.getDocumentNo() + " @Completed@");
                //	Validate return
                MDocType documentType = MDocType.get(commandReceiver.getCtx(), order.getC_DocType_ID());
                if(!Util.isEmpty(documentType.getDocSubTypeSO())
                		&& documentType.getDocSubTypeSO().equals(MOrder.DocSubTypeSO_RMA)) {
                    //	Generate Return
                    MOrder sourceOrder = null;
                    if(order.getRef_Order_ID() != 0) {
                    	sourceOrder = (MOrder) order.getRef_Order();
                    }
                    //	Validate source order
                    if(sourceOrder != null) {
                      List<MInOut> shipments = Arrays.asList(sourceOrder.getShipments());
                      if(shipments.size() > 0) {
                      	ProcessBuilder
      	                	.create(commandReceiver.getCtx())
      	                	.process(InOutGenerate.getProcessId())
      	                	.withTitle(InOutGenerate.getProcessName())
      	                	.withParameter(InOutGenerate.M_WAREHOUSE_ID, sourceOrder.getM_Warehouse_ID())
      	                	.withParameter(InOutGenerate.DOCACTION, DocAction.ACTION_Complete)
      	                	.withSelectedRecordsIds(I_C_Order.Table_ID, Arrays.asList(order.getC_Order_ID()))
      	                	.withoutTransactionClose()
      	                	.execute(trxName);
                      }
                    }
                    //	Generate Invoice
                    ProcessInfo invoiceInformation = null;
                    invoiceInformation = ProcessBuilder
                        	.create(commandReceiver.getCtx())
                        	.process(InvoiceGenerate.getProcessId())
                        	.withTitle(InvoiceGenerate.getProcessName())
                        	.withParameter(InvoiceGenerate.AD_ORG_ID, order.getAD_Org_ID())
                        	.withParameter(InvoiceGenerate.C_ORDER_ID, order.getC_Order_ID())
                        	.withParameter(InvoiceGenerate.DOCACTION, DocAction.ACTION_Complete)
                        	.withoutTransactionClose()
                        	.execute(trxName);
                    //	Validate Credit Memo
                    if(invoiceInformation == null
                    		|| invoiceInformation.getRecord_ID() == 0) {
                    	throw new AdempiereException("@C_Invoice_ID@ @NotFound@");
                    }
                    //	get credit memo
                    MInvoice creditMemo = new MInvoice(commandReceiver.getCtx(), invoiceInformation.getRecord_ID(), trxName);
                    //	Create return
                    createPayment(commandReceiver, MPayment.TENDERTYPE_CreditMemo, order, creditMemo.getDocumentNo(), trxName);
                    commandReceiver.setProcessInfo(processInformation);
                }
            }
        });
    }
    
	/**
	 * Payment with reference No
	 * @param amount
	 * @param currencyId
	 * @param referenceNo
	 * @return true if payment processed correctly; otherwise false
	 */
	private MPayment createPayment(CommandReceiver commandReceiver, String tenderType, MOrder order, String referenceNo, String transactionName) {
		MPayment payment = new MPayment(commandReceiver.getCtx(), 0, transactionName);
		MPOS pos = MPOS.get(commandReceiver.getCtx(), commandReceiver.getPOSId());
		payment.setAD_Org_ID(order.getAD_Org_ID());
		payment.setC_POS_ID(commandReceiver.getPOSId());
		payment.setTenderType(tenderType);
		payment.setIsReceipt(false);
		payment.setC_Order_ID(commandReceiver.getOrderId());
		payment.setIsPrepayment(true);
		payment.setC_BPartner_ID(commandReceiver.getPartnerId());
		payment.setDateTrx(order.getDateOrdered());
		payment.setDateAcct(order.getDateOrdered());
		payment.setCreditCardType(null);
		payment.setC_CashBook_ID(pos.getC_CashBook_ID());
		payment.setAmount(order.getC_Currency_ID(), order.getGrandTotal());
		int conversionTypeId = pos.getC_ConversionType_ID();
		if(conversionTypeId > 0) {
			payment.setC_ConversionType_ID(conversionTypeId);
		}
		payment.setC_BankAccount_ID(pos.getC_BankAccount_ID());
		payment.setDocumentNo(referenceNo);
		payment.saveEx();
		payment.setDocAction(MPayment.DOCACTION_Complete);
		payment.setDocStatus(MPayment.DOCSTATUS_Drafted);
		if(payment.processIt(MPayment.DOCACTION_Complete)) {
			payment.saveEx();
			MBankStatement.addPayment(payment);
			return payment;
		}
		return null;
	} // payDirectDebit
}
