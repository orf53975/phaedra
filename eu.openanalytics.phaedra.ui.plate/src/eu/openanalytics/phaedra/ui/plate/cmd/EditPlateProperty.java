package eu.openanalytics.phaedra.ui.plate.cmd;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.ui.plate.dialog.EditPlatePropertyDialog;

public class EditPlateProperty extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
		execute(plates);
		return null;
	}

	public static void execute(List<Plate> plates) {
		if (plates.isEmpty()) return;
		boolean access = true;
		for (Plate p: plates) {
			access = SecurityService.getInstance().checkWithDialog(Permissions.PLATE_EDIT, p);
			if (!access) break;
		}
		if (access) {
			EditPlatePropertyDialog dialog = new EditPlatePropertyDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), plates);
			dialog.open();
		}
	}
}