/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
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
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.model;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.compiere.util.CCache;
import org.compiere.util.Util;

/**
 * 	Request Mail Template Model.
 *	Cannot be cached as it holds PO/BPartner/User to parse
 *  @author Jorg Janke
 *  @version $Id: MMailText.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class MMailText extends X_R_MailText
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -9121875595478208460L;

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param R_MailText_ID id
	 *	@param trxName transaction
	 */
	public MMailText(Properties ctx, int R_MailText_ID, String trxName)
	{
		super (ctx, R_MailText_ID, trxName);
	}	//	MMailText

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MMailText (Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}	//	MMailText

	/** Parse PO			*/
	private Map<String, PO>	entityMap = new HashMap<>();
	
	/** Translated Header	*/
	private String		m_MailHeader = null;
	/** Translated Text		*/
	private String		m_MailText = null;
	/** Translated Text 2	*/
	private String		m_MailText2 = null;
	/** Translated Text 3	*/
	private String		m_MailText3 = null;
	/** Translation Cache	*/
	private static CCache<String,MMailTextTrl> s_cacheTrl = new CCache<String,MMailTextTrl> ("", 20);
	
	/**
	 * 	Get parsed/translated Mail Text
	 *	@param all concatinate all
	 *	@return parsed/translated text
	 */
	public String getMailText(boolean all)
	{
		if (m_MailText == null)
			translate();
		if (!all)
			return parse(m_MailText);
		//
		StringBuffer sb = new StringBuffer();
		sb.append(m_MailText);
		String s = m_MailText2;
		if (s != null && s.length() > 0)
			sb.append("\n").append(s);
		s = m_MailText3;
		if (s != null && s.length() > 0)
			sb.append("\n").append(s);
		//
		return parse(sb.toString());
	}	//	getMailText

	/**
	 * 	Get parsed/translated Mail Text
	 *	@return parsed/translated text
	 */
	public String getMailText()
	{
		if (m_MailText == null)
			translate();
		return parse (m_MailText);
	}	//	getMailText
	
	/**
	 * 	Get parsed/translated Mail Text 2
	 *	@return parsed/translated text
	 */
	public String getMailText2()
	{
		if (m_MailText == null)
			translate();
		return parse (m_MailText2);
	}	//	getMailText2

	/**
	 * 	Get parsed/translated Mail Text 2
	 *	@return parsed/translated text
	 */
	public String getMailText3()
	{
		if (m_MailText == null)
			translate();
		return parse (m_MailText3);
	}	//	getMailText3

	/**
	 * 	Get parsed/translated Mail Header
	 *	@return parsed/translated text
	 */
	public String getMailHeader()
	{
		if (m_MailHeader == null)
			translate();
		return parse(m_MailHeader);
	}	//	getMailHeader
	
	/**************************************************************************
	 * 	Parse Text
	 *	@param text text
	 *	@return parsed text
	 */
	private String parse (String text)
	{
		if (text == null)
			return "";
		if (text.indexOf('@') == -1)
			return text;
		//	Parse User
		text = parse (text, getEntity(I_AD_User.Table_Name));
		//	Parse BP
		text = parse (text, getEntity(I_C_BPartner.Table_Name));
		//	Parse PO
		text = parse (text, null);
		//
		return text;
	}	//	parse
	
	/**
	 * Get Entity from table name
	 * @param tableName
	 * @return
	 */
	private PO getEntity(String tableName) {
		return entityMap.get(tableName);
	}
	
	/**
	 * 	Parse text
	 *	@param text text
	 *	@param po object
	 *	@return parsed text
	 */
	private String parse (String text, PO po) {
		if (text == null)
			return "";
		if (text.indexOf('@') == -1)
			return text;
		
		String inStr = text;
		String token;
		StringBuffer outStr = new StringBuffer();

		int i = inStr.indexOf('@');
		while (i != -1)
		{
			outStr.append(inStr.substring(0, i));			// up to @
			inStr = inStr.substring(i+1, inStr.length());	// from first @

			int j = inStr.indexOf('@');						// next @
			if (j < 0)										// no second tag
			{
				inStr = "@" + inStr;
				break;
			}

			token = inStr.substring(0, j);
			String value = "";
			int indeOf = token.indexOf(".");
			if(indeOf > 0) {
				String tableName = token.substring(0, indeOf);
				String columnName = token.substring(indeOf + 1, token.length());
				if(!Util.isEmpty(tableName)) {
					value = parseVariable(columnName, entityMap.get(tableName));
				}
			} else if(po != null) {
				value = parseVariable(token, po);	//	replace context
			}
			if(Util.isEmpty(value)) {
				value = "@" + token + "@";
			}
			outStr.append(value);
			inStr = inStr.substring(j+1, inStr.length());	// from second @
			i = inStr.indexOf('@');
		}
		outStr.append(inStr);           					//	add remainder
		return outStr.toString();
	}	//	parse

	/**
	 * 	Parse Variable
	 *	@param variable variable
	 *	@param po po
	 *	@return translated variable or if not found the original tag
	 */
	private String parseVariable (String variable, PO po)
	{
		if (variable == null || po == null)
			return "";
		int index = po.get_ColumnIndex(variable);
		if (index == -1)
			return "@" + variable + "@";	//	keep for next
		//
		Object value = po.get_Value(index);
		if (value == null)
			return "";
		return value.toString();
	}	//	translate
	
	/**
	 * 	Set User for parse
	 *	@param AD_User_ID user
	 */
	public void setUser (int AD_User_ID) {
		setPO(MUser.get (getCtx(), AD_User_ID));
	}	//	setUser
	
	/**
	 * 	Set User for parse
	 *	@param user user
	 */
	public void setUser (MUser user) {
		setPO(user);
	}	//	setUser
	
	/**
	 * 	Set BPartner for parse
	 *	@param C_BPartner_ID bp
	 */
	public void setBPartner (int C_BPartner_ID) {
		setPO(new MBPartner (getCtx(), C_BPartner_ID, get_TrxName()));
	}	//	setBPartner
	
	/**
	 * 	Set BPartner for parse
	 *	@param bpartner bp
	 */
	public void setBPartner (MBPartner bpartner) {
		setPO(bpartner);
	}	//	setBPartner

	/**
	 * 	Set PO for parse
	 *	@param po po
	 */
	public void setPO (PO po) {
		if(po == null) {
			return;
		}
		entityMap.put(po.get_TableName(), po);
	}	//	setPO

	/**
	 * 	Set PO for parse
	 *	@param po po
	 *	@param analyse if set to true, search for BPartner/User
	 */
	public void setPO (PO po, boolean analyse)
	{
		setPO(po);
		if (analyse)
		{
			int index = po.get_ColumnIndex("C_BPartner_ID");
			if (index > 0)
			{
				Object oo = po.get_Value(index);
				if (oo instanceof Integer)
				{
					int C_BPartner_ID = ((Integer)oo).intValue();
					setBPartner(C_BPartner_ID);
				}
			}
			index = po.get_ColumnIndex("AD_User_ID");
			if (index > 0)
			{
				Object oo = po.get_Value(index);
				if (oo instanceof Integer)
				{
					int AD_User_ID = ((Integer)oo).intValue();
					setUser(AD_User_ID);
				}
			}
		}
	}	//	setPO

	/**
	 * 	Translate to BPartner Language
	 */
	private void translate() {
		MBPartner bpartner = (MBPartner) getEntity(I_C_BPartner.Table_Name);
		if (bpartner != null && bpartner.getAD_Language() != null) {
			String key = bpartner.getAD_Language() + get_ID();
			MMailTextTrl trl = s_cacheTrl.get(key);
			if (trl == null) {
				trl = getTranslation(bpartner.getAD_Language());
				if (trl != null)
					s_cacheTrl.put(key, trl);
			}
			if (trl != null) {
				m_MailHeader = trl.MailHeader;
				m_MailText = trl.MailText;
				m_MailText2 = trl.MailText2;
				m_MailText3 = trl.MailText3;
			}
		}
		//	No Translation
		m_MailHeader = super.getMailHeader();
		m_MailText = super.getMailText();
		m_MailText2 = super.getMailText2();
		m_MailText3 = super.getMailText3();
	}	//	translate
	
	/**
	 * 	Get Translation
	 *	@param language language
	 *	@return trl
	 */
	private MMailTextTrl getTranslation (String language) {
		PO translationEntity = new Query(getCtx(), Table_Name + "_Trl", "R_MailText_ID=? AND AD_Language=?", get_TrxName())
				.setParameters(getR_MailText_ID(), language)
				.first();
		//	
		if(translationEntity != null) {
			MMailTextTrl translation = new MMailTextTrl();
			translation.AD_Language = translationEntity.get_ValueAsString("AD_Language");
			translation.MailHeader = translationEntity.get_ValueAsString("MailHeader");
			translation.MailText = translationEntity.get_ValueAsString("MailText");
			translation.MailText2 = translationEntity.get_ValueAsString("MailText2");
			translation.MailText3 = translationEntity.get_ValueAsString("MailText3");
			return translation;
		}
		//	
		return null;
	}	//	getTranslation
	
	/**
	 *	MailText Translation VO
	 */
	class MMailTextTrl {
		/** Language			*/
		String		AD_Language = null;
		/** Translated Header	*/
		String		MailHeader = null;
		/** Translated Text		*/
		String		MailText = null;
		/** Translated Text 2	*/
		String		MailText2 = null;
		/** Translated Text 3	*/
		String		MailText3 = null;
	}	//	MMailTextTrl
	
}	//	MMailText
