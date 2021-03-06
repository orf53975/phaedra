package eu.openanalytics.phaedra.ui.silo.editor;

import java.io.IOException;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.DefaultComparator;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultLongDisplayConverter;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.convert.FormattedDisplayConverter;
import eu.openanalytics.phaedra.base.ui.util.tooltip.ToolTipLabelProvider;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.util.SiloUtils;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.silo.vo.SiloDatasetColumn;
import eu.openanalytics.phaedra.ui.silo.Activator;
import eu.openanalytics.phaedra.ui.wellimage.tooltip.WellToolTipLabelProvider;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

public class WellSiloEditor extends SiloEditor<Well, Feature> {

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
	}

	@Override
	protected float getDefaultScale() {
		return 1f/32;
	}

	@Override
	protected ImageData getImageData(Well entity, float scale, boolean[] enabledChannels) {
		try {
			return ImageRenderService.getInstance().getWellImageData(entity, scale, enabledChannels);
		} catch (IOException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}
		return null;
	}

	@Override
	protected Rectangle getImageBounds(Well entity, float scale) {
		Point size = ImageRenderService.getInstance().getWellImageSize(entity, scale);
		return new Rectangle(0, 0, size.x + 1, size.y + 1);
	}

	@Override
	protected boolean isImageReady(Well entity, float scale, boolean[] channels) {
		return ImageRenderService.getInstance().isWellImageCached(entity, scale, channels);
	}

	@Override
	protected List<Well> getSelectedEntities(ISelection selection) {
		return SelectionUtils.getObjects(selection, Well.class);
	}

	@Override
	protected void registerDisplayConverters(ISiloAccessor<Well> accessor, String dataGroup, IConfigRegistry configRegistry) {
		SiloDataset ds = getDataset();
		for (int i = 0; i < ds.getColumns().size(); i++) {
			SiloDatasetColumn col = ds.getColumns().get(i);
			IFeature f = SiloUtils.getWellFeature(col);
			if (f != null) {
				String formatString = f.getFormatString();
				FormattedDisplayConverter converter = new FormattedDisplayConverter(formatString, false);
				configRegistry.registerConfigAttribute(
						CellConfigAttributes.DISPLAY_CONVERTER
						, converter
						, DisplayMode.NORMAL
						, col.getName()
				);
				NatTableUtils.applyAdvancedFilter(configRegistry, i + 1
						, converter, converter.getFilterComparator());
			} else {
				switch (col.getType()) {
				case Float:
					FormattedDisplayConverter converter = new FormattedDisplayConverter(false);
					configRegistry.registerConfigAttribute(
							CellConfigAttributes.DISPLAY_CONVERTER
							, converter
							, DisplayMode.NORMAL
							, col.getName()
					);
					NatTableUtils.applyAdvancedFilter(configRegistry, i + 1
							, converter, converter.getFilterComparator());
					break;
				case Long:
					DefaultLongDisplayConverter longConverter = new DefaultLongDisplayConverter();
					configRegistry.registerConfigAttribute(
							CellConfigAttributes.DISPLAY_CONVERTER
							, longConverter
							, DisplayMode.NORMAL
							, col.getName()
					);
					NatTableUtils.applyAdvancedFilter(configRegistry, i + 1
							, longConverter, new DefaultComparator());
					break;
				case String:
				case None:
				default:
					break;
				}
			}
		}
	}

	@Override
	protected ToolTipLabelProvider createToolTipLabelProvider() {
		return new WellToolTipLabelProvider();
	}

}