package org.intellij.ibatis.intention;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiClass;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import com.intellij.javaee.dataSource.DatabaseTableData;
import com.intellij.javaee.dataSource.DatabaseTableFieldData;
import org.intellij.ibatis.dom.sqlMap.Insert;
import org.intellij.ibatis.dom.sqlMap.TypeAlias;
import org.intellij.ibatis.provider.IbatisClassShortcutsReferenceProvider;
import org.intellij.ibatis.provider.TableColumnReferenceProvider;
import static org.intellij.ibatis.provider.TableColumnReferenceProvider.getDatabaseTableData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GenerateSQLForInsertAction extends PsiIntentionBase {
	protected void invoke(Project project, Editor editor, PsiFile file, @NotNull PsiElement element) {
		if (isAvailable(project, editor, file)) {
			XmlAttribute attribute;

			// element is the insert tag

			// ok, see if it's a parameter map
			attribute = ((XmlTag) element).getAttribute("parameterMap");
			if (null != attribute) {
				XmlAttributeValue xmlAttributeValue = attribute.getValueElement();
				if (null != xmlAttributeValue) {
					PsiReference psiReference = xmlAttributeValue.getReference();
					if (null != psiReference) {
						PsiElement psiElement = psiReference.resolve();
						if (psiElement != null && psiElement instanceof XmlAttribute) {

						}
					}
				}
			}

			// ok, then see if it's a parameter class
			attribute = ((XmlTag) element).getAttribute("parameterClass");
			if (null != attribute) {
				XmlAttributeValue xmlAttributeValue = attribute.getValueElement();
				if (null != xmlAttributeValue) {
					PsiReference psiReference = xmlAttributeValue.getReference();
					if (null != psiReference) {
						PsiElement psiElement = psiReference.resolve();
						XmlTag typeAliasTag = (XmlTag) psiElement;
						DomElement typeAliasTemp = DomManager.getDomManager(project).getDomElement(typeAliasTag);

						if (typeAliasTemp != null && typeAliasTemp instanceof TypeAlias) {
							TypeAlias ta = (TypeAlias) typeAliasTemp;
							PsiClass value = ta.getType().getValue();
							if(null!=value){
								String className = value.getQualifiedName();
								DatabaseTableData tableData = getDatabaseTableData(value);
								if(null != tableData){
									List<DatabaseTableFieldData> fieldList = tableData.getFields();

									PsiClass psiClass = IbatisClassShortcutsReferenceProvider.getPsiClass(psiElement, className);
									// ok, now we have the table meta-data and the class meta-data.
									// now we can build our insert statement
									StringBuilder insertStatement = new StringBuilder("\ninsert into ").append(tableData.getName());
									StringBuilder insertList = new StringBuilder("");
									StringBuilder valueList = new StringBuilder("");
									for (DatabaseTableFieldData d : fieldList) {
										String propName = TableColumnReferenceProvider.getPropNameForColumn(psiClass, d);
										if(null != propName){
											if(insertList.length() == 0) {
												insertList.append(" (");
											}else{
												insertList.append(", ");
											}
											if(valueList.length() == 0) {
												valueList.append("\nvalues (");
											}else{
												valueList.append(", ");
											}
											insertList.append(d.getName());
											valueList.append("#").append(propName).append("#");
										}
									}
									if(insertList.length() > 0){
										// ok, build the SQL statement...
										XmlTag xmlTag = (XmlTag) element;
										xmlTag.getValue().setText(insertStatement.append(insertList).append(") ").append(valueList).append(")").toString());
									}
								}
							}
						}
					}
				}
				
			}
		}
	}

	protected boolean isAvailable(Project project, Editor editor, PsiFile file, @NotNull PsiElement element) {
		// the default answer is no
		boolean returnValue = false;

		if (file instanceof XmlFile && element instanceof XmlTag) {
			XmlTag xmlTag = (XmlTag) element;
			if (xmlTag.getName().equals("insert") && xmlTag.getValue().getText().trim().length() == 0) {
				// we are looking at an empty insert tag
				if (xmlTag.getAttributeValue("parameterMap") != null) {
					// we have a parameter map
					DomElement domElement = DomManager.getDomManager(project).getDomElement(xmlTag);
					if (domElement != null && domElement instanceof Insert) {
						// we have an insert tag w/ a parameter map
						// todo: what if the parameter is a Map?
						returnValue = true;
					}
				} else if (xmlTag.getAttributeValue("parameterClass") != null) {
					// we have a parameter class
					DomElement domElement = DomManager.getDomManager(project).getDomElement(xmlTag);
					if (domElement != null && domElement instanceof Insert) {
						// we have an insert tag w/ a parameter class
						// todo: what if the parameter is a Map?
						returnValue = true;
					}
				}
			}
		}

		return returnValue;
	}

	@NotNull
	public String getText() {
		return "Generate SQL for an insert based on parameter class or parameter map";
	}

	@NotNull
	public String getFamilyName() {
		return "GenerateSqlForInsert";
	}
}
