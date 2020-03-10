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

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_Order;
import org.compiere.model.MOrder;
import org.compiere.process.DocAction;
import org.compiere.process.InvoiceGenerate;
import org.compiere.process.ProcessInfo;
import org.compiere.util.Trx;
import org.compiere.util.TrxRunnable;
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
                //	Generate Return
//                boolean isDelivered = false;
//                List<MInOut> shipments = Arrays.asList(sourceOrder.getShipments());
//                if(isDelivered) {
//                	ProcessBuilder
//	                	.create(getCtx())
//	                	.process(InOutGenerate.getProcessId())
//	                	.withTitle(InOutGenerate.getProcessName())
//	                	.withParameter(InOutGenerate.M_WAREHOUSE_ID, sourceOrder.getM_Warehouse_ID())
//	                	.withParameter(InOutGenerate.DOCACTION, DocAction.ACTION_Complete)
//	                	.withSelectedRecordsIds(I_C_Order.Table_ID, Arrays.asList(returnOrder.getC_Order_ID()))
//	                	.withoutTransactionClose()
//	                	.execute(get_TrxName());
//                }
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
                commandReceiver.setProcessInfo(processInformation);
            }
        });
    }
}
