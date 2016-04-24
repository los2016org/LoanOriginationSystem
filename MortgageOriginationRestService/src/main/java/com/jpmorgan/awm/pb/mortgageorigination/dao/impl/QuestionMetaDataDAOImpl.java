package com.jpmorgan.awm.pb.mortgageorigination.dao.impl;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpmorgan.awm.pb.mortgageorigination.dao.QuestionMetaDataDAO;
import com.myorg.losmodel.model.questions.Attribute;
import com.myorg.losmodel.model.questions.DataType;
import com.myorg.losmodel.model.questions.LookupListOfValues;
import com.myorg.losmodel.model.questions.Question;
import com.myorg.losmodel.model.questions.QuestionContext;
import com.myorg.losmodel.model.questions.Role;
import com.myorg.losmodel.model.questions.Section;

@Service
public class QuestionMetaDataDAOImpl implements QuestionMetaDataDAO {

	@Autowired
	private DataSource dataSource;

	/**
	 * Method to get the sections / questions / attributes / LOVS for a user
	 * based on specified language or default language if language is not
	 * selected
	 * 
	 * NOTE: The language must be for the user of Role Customer. Otherwise the
	 * glass would break Ensure that the userid passed is the userid of the
	 * customer
	 * 
	 * @param conn
	 * @param languageCd
	 * @param userCd
	 * @return
	 * @throws SQLException
	 */
	public Set<Section> questionDAOMethod(String languageCd, String userCd) throws SQLException {

		Class.forName("oracle.jdbc.driver.OracleDriver");
		java.sql.Connection conn = null;
		// Question question = null;

		try {

			conn = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:XE", "mortgage", "password");

			TreeSet<Section> sectionSet = new TreeSet<Section>();
			conn.setAutoCommit(false);

			PreparedStatement questionPS = null;

			PreparedStatement sectionPS = null;

			sectionPS = conn.prepareStatement(
					"select s.section_id,s.present_section_nm, s.past_section_nm, s.future_section_nm, s.sequence_no,"
							+ " s.parent_section_id as parent_id,"
							+ " sin.language_iso2_cd, sin.present_section_nm as i18n_present_nm, sin.past_section_nm as i18N_past_nm,sin.future_section_nm as i18n_future_nm "
							+ " from mortgage.section_metadata s"
							+ " left outer join mortgage.section_metadata_i18n sin on sin.section_id = s.section_id");

			ResultSet sectionRS = sectionPS.executeQuery();

			while (sectionRS.next()) {

				// First we will create a new section with the section id from
				// the resultset
				Section sectionFromResultSet = new Section();
				int sectionId = sectionRS.getInt("section_id");
				sectionFromResultSet.setSectionId(sectionId);
				// System.out.println("Section ID"+sectionId);

				// As we are building the sections, we will use the equals
				// method to see the section is already in the collection
				// created in a previous
				// iteration
				Section sectionFromCollection = findSectionInSet(sectionFromResultSet, sectionSet);
				if (sectionFromCollection == null) {
					sectionFromCollection = sectionFromResultSet;
					// If the section is not already in my set, I add it
					sectionSet.add(sectionFromCollection);
				}

				// this will be repeated - a wasted call. Opportunity to
				// optimize in the future

				String enPresentNm = sectionRS.getString("present_section_nm");
				String enPastNm = sectionRS.getString("past_section_nm");
				String enFutureNm = sectionRS.getString("future_section_nm");

				int sequenceNo = sectionRS.getInt("sequence_no");
				sectionFromCollection.setPresentSectionNm("en", enPresentNm);
				sectionFromCollection.setPastSectionNm("en", enPastNm);
				sectionFromCollection.setFutureSectionNm("en", enFutureNm);
				sectionFromCollection.setSequenceNo(sequenceNo);

				int parentSectionId = sectionRS.getInt("parent_id");
				// System.out.println("Parent Section ID"+parentSectionId);

				// It is important we know the children of a section. However
				// initially we would try to know the parent. Later on we will
				// do it upside down to get
				// the levels and the children
				Section parentFromResultSet = null;

				if (parentSectionId > 0) {
					parentFromResultSet = new Section();
					parentFromResultSet.setSectionId(parentSectionId);
				}
				// If the parent is not already in the collection. create it.
				// Take advantage of the equals method

				Section parentFromSet = null;
				if (parentFromResultSet != null) {
					parentFromSet = this.findSectionInSet(parentFromResultSet, sectionSet);
				}
				if (parentFromSet == null) {
					parentFromSet = parentFromResultSet;
					if (parentFromSet != null) {
						sectionSet.add(parentFromSet);
					}
				}

				sectionFromCollection.setParentSection(parentFromSet);
				String languageCdSection = sectionRS.getString("language_iso2_cd");
				String i18NPresentNm = sectionRS.getNString("i18n_present_nm");
				String i18NPastNm = sectionRS.getNString("i18n_past_nm");
				String i18NFutureNm = sectionRS.getNString("i18n_future_nm");

				// Since we have an outer join it can come as null. We do not
				// want to add nulls in the object
				if (languageCdSection != null) {
					if (i18NPresentNm != null) {
						sectionFromCollection.setPresentSectionNm(languageCdSection, i18NPresentNm);
					}
					if (i18NPastNm != null) {
						sectionFromCollection.setPastSectionNm(languageCdSection, i18NPastNm);

					}
					if (i18NFutureNm != null) {
						sectionFromCollection.setFutureSectionNm(languageCdSection, i18NFutureNm);
					}

				}

			}

			Iterator<Section> i = sectionSet.iterator();
			/*
			 * while(i.hasNext()){ Section check = i.next();
			 * System.out.println("Section"+check.getSectionId()); Section
			 * parent = check.getParentSection(); if (parent == null){
			 * System.out.println("Parent is null"); }else{ System.out.println(
			 * "Parent is "+parent.getSectionId()); }
			 * 
			 * }
			 */
			// When we are here all the sections know who is their parent, but
			// we also need to know the level
			// and list of children
			// So we pass the collection to a helper method to get all the
			// children

			computeChildrenAndLevelsForSection(sectionSet);
			// Now we have the sections

			PreparedStatement languageCdPS = conn
					.prepareStatement("select default_language_cd from mortgage.users where user_cd = ?");

			if ("default".equals(languageCd) || languageCd == null) {
				languageCdPS.setString(1, userCd);
				ResultSet defaultLangRs = languageCdPS.executeQuery();
				while (defaultLangRs.next()) {
					languageCd = defaultLangRs.getString("default_language_cd");
				}

			}

			if (languageCd == null) {
				languageCd = "en";

			}

			if ("en".equals(languageCd)) {
				questionPS = conn.prepareStatement(
						"select q.question_id,q.question_long_desc,q.section_id,q.parent_question_id,q.role_id,q.question_context_help_desc,"
								+ " q.mandatory_cd,q.sequence_no,r.ROLE_NM,q.QUESTION_CONTEXT_ID,qc.QUESTION_CONTEXT_DESC,null as q_long_desc_i18n, null as q_context_help_desc_i18n"
								+ " from mortgage.question_metadata q"
								+ " inner join role_metadata r on r.role_id = q.role_id"
								+ " inner join question_context_metadata qc on q.QUESTION_CONTEXT_ID = qc.question_context_id"

								+ " where q.logical_del_fl = 'N'");
			} else {

				questionPS = conn.prepareStatement(
						"select q.question_id,q.question_long_desc,q.section_id,q.parent_question_id,q.role_id,q.question_context_help_desc,"
								+ " q.mandatory_cd,q.sequence_no,r.ROLE_NM,q.QUESTION_CONTEXT_ID,qc.QUESTION_CONTEXT_DESC,qi.question_long_desc as q_long_desc_i18n, qi.question_context_help_desc as q_context_help_desc_i18n"
								+ " from mortgage.question_metadata q"
								+ " inner join role_metadata r on r.role_id = q.role_id"
								+ " inner join question_context_metadata qc on q.QUESTION_CONTEXT_ID = qc.question_context_id"
								+ " left outer join question_attribute_i18n qi on q.question_id = qi.question_id"
								+ " where q.logical_del_fl = 'N' and qi.language_iso2_cd = ? ");
			}

			TreeSet<Question> questions = new TreeSet<Question>();
			// System.out.println(languageCd);
			if (!"en".equals(languageCd)) {
				questionPS.setString(1, languageCd);
			}
			ResultSet questionRs = questionPS.executeQuery();

			while (questionRs.next()) {
				// System.out.println("1");
				Question q = new Question();
				q.setQuestionId(questionRs.getInt("question_id"));
				String questionDescI18N = questionRs.getNString("q_long_desc_i18n");
				String questionDesc = questionRs.getString("question_long_desc");
				if (!((questionDescI18N == null) || ("".equals(questionDescI18N)))) {
					questionDesc = questionDescI18N;
				}
				q.setQuestionLongDesc(questionDesc);

				String toolTipI18N = questionRs.getNString("q_context_help_desc_i18n");
				String toolTip = questionRs.getString("question_context_help_desc");
				if (!((toolTipI18N == null) || ("".equals(toolTipI18N)))) {
					toolTip = toolTip;
				}

				q.setToolTip(toolTip);

				// System.out.println(q.getQuestionId()+q.getQuestionLongDesc());

				Section questionSectionFromRS = new Section();
				questionSectionFromRS.setSectionId(questionRs.getInt("section_id"));

				Section questionSectionFromSet = findSectionInSet(questionSectionFromRS, sectionSet);
				questionSectionFromSet.setActiveLanguage(languageCd);
				q.setSection(questionSectionFromSet);
				questionSectionFromSet.addQuestion(q);

				Question parentQuestionFromResultSet = null;
				int parentQuestionId = questionRs.getInt("parent_question_id");
				if (parentQuestionId > 0) {
					parentQuestionFromResultSet = new Question();
					parentQuestionFromResultSet.setQuestionId(parentQuestionId);
				}
				// If the parent is not already in the collection. create it.
				// Take advantage of the equals method

				Question parentQuestionFromSet = null;
				if (parentQuestionFromResultSet != null) {
					parentQuestionFromSet = findQuestionInSet(parentQuestionFromResultSet, questions);
				}
				if (parentQuestionFromSet == null) {
					parentQuestionFromSet = parentQuestionFromResultSet;
					if (parentQuestionFromSet != null) {
						questions.add(parentQuestionFromSet);
					}
				}

				Role r = new Role();
				r.setRoleId(questionRs.getInt("role_id"));
				r.setRoleNm(questionRs.getString("role_nm"));

				q.setRole(r);

				QuestionContext qc = new QuestionContext();
				qc.setQuestionContextId(questionRs.getInt("question_context_id"));
				qc.setQuestionContextNm(questionRs.getString("QUESTION_CONTEXT_DESC"));
				q.setQuestionContext(qc);

				q.setMandatoryCd(questionRs.getString("mandatory_cd"));
				q.setSequenceNo(questionRs.getInt("sequence_no"));

				// When we are here all the sections know who is their parent,
				// but we also need to know the
				// list of children
				// So we pass the collection to a helper method to get all the
				// children

				computeChildrenForQuestion(questions);

				// All questions read

				// Now let get the attributes and add them to the questions
				PreparedStatement attrPs = null;
				if (!"en".equals(languageCd)) {
					attrPs = conn.prepareStatement(

							" select m.question_id,a.attribute_id,a.col_nm,a.col_desc,a.datatype_id,d.DATATYPE_NM,"
									+ "a.lookup_entity_nm,i.LANGUAGE_ISO2_CD, i.COL_DESC as col_desc_i18n, m.sequence_no "
									+ "from mortgage.attribute_metadata a "
									+ "left outer join mortgage.attribute_i18n i on a.attribute_id = i.attribute_id "
									+ "inner join mortgage.question_attribute_map m on m.col_id = a.attribute_id "
									+ "inner join mortgage.datatype_metadata d on d.datatype_id = a.datatype_id "
									+ "where a.logical_del_fl = 'N' and i.language_iso2_cd = ?");

					attrPs.setString(1, languageCd);
				} else {

					attrPs = conn.prepareStatement(

							" select m.question_id,a.attribute_id,a.col_nm,a.col_desc,a.datatype_id,d.DATATYPE_NM,"
									+ "a.lookup_entity_nm, null as LANGUAGE_ISO2_CD, null as col_desc_i18n,m.sequence_no "
									+ "from mortgage.attribute_metadata a "

									+ "inner join mortgage.question_attribute_map m on m.col_id = a.attribute_id "
									+ "inner join mortgage.datatype_metadata d on d.datatype_id = a.datatype_id "
									+ "where a.logical_del_fl = 'N' ");
				}
				ResultSet attrRs = attrPs.executeQuery();

				HashSet<Attribute> attributes = new HashSet<Attribute>();
				while (attrRs.next()) {
					Attribute attrFromResultSet = new Attribute();
					attrFromResultSet.setAttributeId(attrRs.getInt("attribute_id"));
					Question questionFromResultSet = new Question();
					questionFromResultSet.setQuestionId(attrRs.getInt("question_id"));

					Question questionFromSet = null;
					if (questionFromResultSet != null) {
						questionFromSet = findQuestionInSet(questionFromResultSet, questions);
					}
					if (questionFromSet == null) {
						questionFromSet = questionFromResultSet;
					}

					questionFromSet.addAttribute(attrFromResultSet);
					attrFromResultSet.setColName(attrRs.getString("col_nm"));

					String attrDescI18N = attrRs.getNString("col_desc_i18n");
					String attrDesc = attrRs.getString("col_desc");
					if (!((attrDescI18N == null) || ("".equals(attrDescI18N)))) {
						attrDesc = attrDescI18N;
					}
					attrFromResultSet.setColDesc(attrDesc);
					int datatypeId = attrRs.getInt("datatype_id");
					String datatypeNm = attrRs.getString("DATATYPE_NM");
					DataType dt = new DataType();
					dt.setDataTypeId(datatypeId);
					dt.setDataTypeNm(datatypeNm);
					attrFromResultSet.setDataType(dt);
					attrFromResultSet.setSequenceNo(attrRs.getInt("sequence_no"));
					attrFromResultSet.setLookupEntityNm(attrRs.getString("lookup_entity_nm"));

				}

				// Here I have the list of attributes - only thing missing is
				// the List of values

				int index = 1;
				PreparedStatement ps4 = null;
				if ("en".equals(languageCd)) {
					ps4 = conn.prepareStatement(
							"select r.lookup_entity_nm, r.lookup_cd,r.lookup_desc, r.sort_order_no,null as lookup_desc_i18n from mortgage.reference_lookup r"

									+ " where r.logical_del_fl = 'N' and r.lookup_entity_nm = ?");

				} else {

					ps4 = conn.prepareStatement(
							"select r.lookup_entity_nm, r.lookup_cd,r.lookup_desc, r.sort_order_no,i.lookup_desc as lookup_desc_i18n from mortgage.reference_lookup r"
									+ " left outer join mortgage.reference_lookup_i18n i on i.lookup_entity_nm = r.lookup_entity_nm and r.lookup_cd = i.lookup_cd"
									+ " where r.logical_del_fl = 'N' and i.language_iso2_cd = ? and r.lookup_entity_nm = ?");
					index = 2;
					ps4.setString(1, languageCd);

				}

				Iterator<Attribute> it = attributes.iterator();
				while (it.hasNext()) {
					Attribute a = it.next();
					ps4.setString(index, a.getLookupEntityNm());
					ResultSet lookupRs = ps4.executeQuery();
					while (lookupRs.next()) {

						LookupListOfValues lov = new LookupListOfValues();
						lov.setLookupCd(lookupRs.getString("lookupCd"));

						String lookupDescI18N = attrRs.getNString("lookup_desc_i18n");
						String lookupDesc = attrRs.getString("lookup_desc");
						if (!((lookupDescI18N == null) || ("".equals(lookupDescI18N)))) {
							lookupDesc = lookupDescI18N;
						}
						lov.setLookupValue(lookupDesc);
						lov.setSortOrder(lookupRs.getInt("sort_order_no"));

						a.addListOfValues(lov);
					}

				}

				// Now - Section contains questions - question contains both
				// section and attributes, attributes contain LOV

			}
			conn.commit();

		}

		catch (Exception ee) {
			try {
				conn.rollback();
			} catch (Exception e) {
				throw new SQLException("ROLLBACK FAILLED", e);

			}
			throw new SQLException("ROLLED BACK DUE TO EXCEPTION", ee);
		}

		return sectionSet;

	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

}