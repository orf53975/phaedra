package eu.openanalytics.phaedra.base.ui.richtableviewer.util;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;

public class CopyRowHelper {

	public static void copyToClipboard(Table table, Clipboard clipboard) {
		StringBuilder sb = new StringBuilder();
		TableColumn[] cols = table.getColumns();
		TableItem[] items = table.getSelection();

		if (items.length == table.getItemCount()) {
			// Full selection: include headers.
			for (int c = 0; c < cols.length; c++) {
				if (cols[c].getWidth() > 0) {
					String text = cols[c].getText();
					if (text == null)
						text = "";
					sb.append(text);
					if (c + 1 < cols.length)
						sb.append("\t");
				}
			}
			sb.append("\n");
		}

		for (int i = 0; i < items.length; i++) {
			for (int c = 0; c < cols.length; c++) {
				if (cols[c].getWidth() > 0) {
					String text = items[i].getText(c);
					if (text == null || text.isEmpty()) {
						Object viewerColumn = table.getColumn(c).getData("org.eclipse.jface.columnViewer");
						if (viewerColumn != null) {
							Object labelProvider = ReflectionUtils.invoke("getLabelProvider", viewerColumn);
							if (labelProvider != null) {
								text = (String)ReflectionUtils.invoke("getText", labelProvider, new Object[]{items[i].getData()}, new Class<?>[]{Object.class});
							}
						}
					}
					if (text == null)
						text = "";
					sb.append(text);
					if (c + 1 < cols.length)
						sb.append("\t");
				}
			}
			sb.append("\n");
		}

		String data = sb.toString();
		if (!data.isEmpty()) {
			clipboard.setContents(new Object[] { data },
					new Transfer[] { TextTransfer.getInstance() });
		}
	}
}
