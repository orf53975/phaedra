package eu.openanalytics.phaedra.ui.export.wizard.pages;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.export.core.ExportSettings;
import eu.openanalytics.phaedra.export.core.filter.LibraryFilter;
import eu.openanalytics.phaedra.export.core.filter.QualifierFilter;
import eu.openanalytics.phaedra.ui.export.Activator;
import eu.openanalytics.phaedra.ui.export.widget.EuroCalendarCombo;
import eu.openanalytics.phaedra.ui.export.wizard.BaseExportWizardPage;

public class FilterPlatesPage extends BaseExportWizardPage {

	private Combo libraryCombo;
	private Combo qualifierCombo;
	
	private Button filterOnValidationChk;
	private Text validationUserTxt;
	private EuroCalendarCombo validationDateFromCombo;
	private EuroCalendarCombo validationDateToCombo;
	
	private Button filterOnApprovalChk;
	private Text approvalUserTxt;
	private EuroCalendarCombo approvalDateFromCombo; 
	private EuroCalendarCombo approvalDateToCombo;
	
	private Button invalidPlatesChk;
	private Button disapprovedPlatesChk;
	
	public FilterPlatesPage() {
		super("Filter Plates");
		setDescription("Step 2/4: Select the plates you want to export.");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(10,10).numColumns(2).applyTo(container);
		setControl(container);

		Label label = new Label(container, SWT.NONE);
		label.setText("Library:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(label);

		libraryCombo = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(libraryCombo);
		
		label = new Label(container, SWT.NONE);
		label.setText("Plate Qualifier:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		qualifierCombo = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().applyTo(qualifierCombo);

		label = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2,1).applyTo(label);
		
		filterOnValidationChk = new Button(container, SWT.CHECK);
		filterOnValidationChk.setText("Filter on Validation");
		GridDataFactory.fillDefaults().span(2,1).applyTo(filterOnValidationChk);
		
		filterOnValidationChk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean checked = filterOnValidationChk.getSelection();
				validationUserTxt.setEnabled(checked);
				validationDateFromCombo.setEnabled(checked);
				validationDateToCombo.setEnabled(checked);
			}
		});
		
		label = new Label(container, SWT.NONE);
		label.setText("Validated by User:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		validationUserTxt = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().applyTo(validationUserTxt);
		validationUserTxt.setEnabled(false);
		
		label = new Label(container, SWT.NONE);
		label.setText("Validation Date:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		Composite subContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(subContainer);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(subContainer);
		
		label = new Label(subContainer, SWT.NONE);
		label.setText("From:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		validationDateFromCombo = new EuroCalendarCombo(subContainer, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(validationDateFromCombo);
		validationDateFromCombo.setEnabled(false);
		
		label = new Label(subContainer, SWT.NONE);
		label.setText("To:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		validationDateToCombo = new EuroCalendarCombo(subContainer, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(validationDateToCombo);
		validationDateToCombo.setEnabled(false);
		
		label = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2,1).applyTo(label);
		
		filterOnApprovalChk = new Button(container, SWT.CHECK);
		filterOnApprovalChk.setText("Filter on Approval");
		GridDataFactory.fillDefaults().span(2,1).applyTo(filterOnApprovalChk);
		
		filterOnApprovalChk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean checked = filterOnApprovalChk.getSelection();
				approvalUserTxt.setEnabled(checked);
				approvalDateFromCombo.setEnabled(checked);
				approvalDateToCombo.setEnabled(checked);
			}
		});
		
		label = new Label(container, SWT.NONE);
		label.setText("Approved by User:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		approvalUserTxt = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().applyTo(approvalUserTxt);
		approvalUserTxt.setEnabled(false);
		
		label = new Label(container, SWT.NONE);
		label.setText("Approval Date:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		subContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(subContainer);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(subContainer);
		
		label = new Label(subContainer, SWT.NONE);
		label.setText("From:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		approvalDateFromCombo = new EuroCalendarCombo(subContainer, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(approvalDateFromCombo);
		approvalDateFromCombo.setEnabled(false);
		
		label = new Label(subContainer, SWT.NONE);
		label.setText("To:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		approvalDateToCombo = new EuroCalendarCombo(subContainer, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(approvalDateToCombo);
		approvalDateToCombo.setEnabled(false);
		
		label = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2,1).applyTo(label);
		
		
		invalidPlatesChk = new Button(container, SWT.CHECK);
		invalidPlatesChk.setText("Include invalidated plates");
		GridDataFactory.fillDefaults().span(2,1).applyTo(invalidPlatesChk);
		
		disapprovedPlatesChk = new Button(container, SWT.CHECK);
		disapprovedPlatesChk.setText("Include disapproved plates");
		GridDataFactory.fillDefaults().span(2,1).applyTo(disapprovedPlatesChk);
		
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		invalidPlatesChk.setSelection(settings.getBoolean("invalidPlatesChk"));
		disapprovedPlatesChk.setSelection(settings.getBoolean("disapprovedPlatesChk"));
	}
	
	@Override
	protected void pageAboutToShow(ExportSettings settings, boolean firstTime) {
		
		if (!firstTime) return;
		
		libraryCombo.setItems(new String[] {"<All>"});
		libraryCombo.setData("loaded", Boolean.FALSE);
		libraryCombo.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if ((Boolean) libraryCombo.getData("loaded")) return;
				String[] libraries = new LibraryFilter(settings.experiments).getLibraries();
				libraryCombo.setItems(libraries);
				libraryCombo.setData("loaded", Boolean.TRUE);
				if (libraryCombo.getItemCount() > 0) libraryCombo.select(0);
			}
		});
		if (libraryCombo.getItemCount() > 0) libraryCombo.select(0);
		
		qualifierCombo.setItems(new String[] {"<All>"});
		qualifierCombo.setData("loaded", Boolean.FALSE);
		qualifierCombo.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if ((Boolean) qualifierCombo.getData("loaded")) return;
				String[] qualifiers = new QualifierFilter(settings.experiments).getQualifiers();
				qualifierCombo.setItems(qualifiers);
				qualifierCombo.setData("loaded", Boolean.TRUE);
				if (qualifierCombo.getItemCount() > 0) qualifierCombo.select(0);
			}
		});
		if (qualifierCombo.getItemCount() > 0) qualifierCombo.select(0);
	}

	@Override
	public void collectSettings(ExportSettings settings) {
		int index = libraryCombo.getSelectionIndex();
		if (index != -1) settings.library = libraryCombo.getItem(index);
		
		index = qualifierCombo.getSelectionIndex();
		if (index != -1) settings.plateQualifier = qualifierCombo.getItem(index);
		
		settings.filterValidation = filterOnValidationChk.getSelection();
		settings.validationUser = validationUserTxt.getText();
		Calendar date = validationDateFromCombo.getDate();
		if (date != null) settings.validationDateFrom = date.getTime();
		date = validationDateToCombo.getDate();
		if (date != null) settings.validationDateTo = date.getTime();
		if (settings.validationDateFrom != null && settings.validationDateTo == null) {
			settings.validationDateTo = new Date();
		}
		if (settings.validationDateTo != null && settings.validationDateFrom == null) {
			settings.validationDateFrom = new Date();
		}
		
		settings.filterApproval = filterOnApprovalChk.getSelection();
		settings.approvalUser = approvalUserTxt.getText();
		date = approvalDateFromCombo.getDate();
		if (date != null) settings.approvalDateFrom = date.getTime();
		date = approvalDateToCombo.getDate();
		if (date != null) settings.approvalDateTo = date.getTime();	
		if (settings.approvalDateFrom != null && settings.approvalDateTo == null) {
			settings.approvalDateTo = new Date();
		}
		if (settings.approvalDateTo != null && settings.approvalDateFrom == null) {
			settings.approvalDateFrom = new Date();
		}
		
		settings.includeInvalidatedPlates = invalidPlatesChk.getSelection();
		settings.includeDisapprovedPlates = disapprovedPlatesChk.getSelection();
		
		IDialogSettings dialogSettings = Activator.getDefault().getDialogSettings();
		dialogSettings.put("invalidPlatesChk", settings.includeInvalidatedPlates);
		dialogSettings.put("disapprovedPlatesChk", settings.includeDisapprovedPlates);
	}
}