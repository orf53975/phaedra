package eu.openanalytics.phaedra.ui.export.subwell.wizard.pages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.util.misc.FileSelector;
import eu.openanalytics.phaedra.export.core.subwell.ExportSettings;
import eu.openanalytics.phaedra.export.core.subwell.IExportWriter;

public class FileLocationPage extends WizardPage implements IWizardPage {

	public static final String PAGE_NAME = "File Location";
	
	private FileSelector fileSelector;
	
	private ExportSettings settings;

	public FileLocationPage(ExportSettings settings) {
		super(PAGE_NAME);
		setTitle(PAGE_NAME);
		setDescription("Select a location to which the file will be saved");
		
		this.settings = settings;
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(container);
		
		String[] extensions;
		if (settings.getExportMode() == IExportWriter.MODE_PAGE_PER_WELL) {
			extensions = new String[] { "*.xlsx" };
		} else {
			extensions = new String[] { "*.xlsx", "*.csv", "*.h5" };
		}
		fileSelector = new FileSelector(container, SWT.SAVE, extensions);
		fileSelector.setSelectedFile(null);
		fileSelector.setSuggestedName("export.xlsx");
		fileSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				settings.setFileLocation(fileSelector.getSelectedFile());
				checkPageComplete();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fileSelector);
		
		checkPageComplete();
		setControl(container);
	}
	
	public void updateFileSelectorExtensions(String[] extensions) {
		fileSelector.setExtensions(extensions);
	}
	
	private void checkPageComplete() {
		setPageComplete(settings.getFileLocation() != null);
	}

}