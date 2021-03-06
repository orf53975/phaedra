package eu.openanalytics.phaedra.ui.silo.navigator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchActionConstants;

import eu.openanalytics.phaedra.base.security.PermissionDeniedException;
import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.silo.dialog.EditSiloDialog;

public class SiloHandler extends BaseHandler {

	@Override
	public boolean matches(IElement element) {
		return (element.getId().startsWith(SiloProvider.ELEMENT_PREFIX_SILO));
	}

	@Override
	public void handleDoubleClick(IElement element) {
		Silo silo = (Silo) element.getData();
		EditorFactory.getInstance().openEditor(silo);
	}

	@Override
	public void createContextMenu(final IElement element, IMenuManager mgr) {
		Action action = new Action("Open Silo", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				handleDoubleClick(element);
			}
		};
		action.setImageDescriptor(IconManager.getIconDescriptor("silo_go.png"));
		mgr.add(action);

		action = new Action("Edit Silo", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				Silo silo = (Silo) element.getData();
				EditSiloDialog dialog = new EditSiloDialog(Display.getDefault().getActiveShell(), silo);
				dialog.open();
			}
		};
		action.setImageDescriptor(IconManager.getIconDescriptor("silo_well_edit.png"));
		mgr.add(action);

		action = new Action("Duplicate Silo", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				Silo silo = (Silo) element.getData();
				Silo siloCopy = SiloService.getInstance().cloneSilo(silo);
				EditSiloDialog dialog = new EditSiloDialog(Display.getDefault().getActiveShell(), siloCopy);
				if (dialog.open() == Window.CANCEL) {
					SiloService.getInstance().deleteSilo(siloCopy);
				}
			}
		};
		action.setImageDescriptor(IconManager.getIconDescriptor("silo_well_edit.png"));
		mgr.add(action);

		action = new Action("Delete Silo", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				Silo silo = (Silo) element.getData();
				boolean confirmed = MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
						"Delete Silo", "Are you sure you want to delete the silo \"" + silo.getName() + "\" ?");
				if (confirmed) {
					try {
						SiloService.getInstance().deleteSilo(silo);
					} catch (PermissionDeniedException ex) {
						MessageDialog.openError(Display.getDefault().getActiveShell(),
								"Delete Error", ex.getMessage());
					}
				}
			}
		};
		action.setImageDescriptor(IconManager.getIconDescriptor("silo_delete.png"));
		mgr.add(action);

		// Chart Menu entry.
		mgr.add(new Separator("siloChartMenu"));

		mgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		
		addCreateCmds(element, mgr);
	}

	@Override
	public void dragStart(IElement element, DragSourceEvent event) {
		event.doit = true;
		event.detail = DND.DROP_COPY;
	}

	@Override
	public void dragSetData(IElement element, DragSourceEvent event) {
		Object data = element.getData();
		LocalSelectionTransfer.getTransfer().setSelection(new StructuredSelection(data));
	}

}