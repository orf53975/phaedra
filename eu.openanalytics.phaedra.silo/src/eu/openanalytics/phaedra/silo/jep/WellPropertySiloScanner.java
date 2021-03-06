package eu.openanalytics.phaedra.silo.jep;

import eu.openanalytics.phaedra.base.scripting.jep.parse.BaseScanner;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.silo.Activator;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;

public class WellPropertySiloScanner extends BaseScanner<SiloDataset> {

	private final static char VAR_SIGN = '@';

	@Override
	protected boolean isValidObject(Object obj) {
		return obj instanceof SiloDataset;
	}

	@Override
	protected char getVarSign() {
		return VAR_SIGN;
	}

	@Override
	protected Object getValueForRef(String scope, String[] fieldNames, SiloDataset dataset) {
		WellProperty prop = WellProperty.getByName(fieldNames[0]);

		if (prop == null) {
			throw new CalculationException("No well property with name: " + fieldNames[0]);
		}

		Silo silo = dataset.getSilo();
		ISiloAccessor<?> accessor = SiloService.getInstance().getSiloAccessor(silo);

		try {
			int rows = accessor.getRowCount(dataset.getName());
			if (silo.getType() == GroupType.WELL.getType()) {
				if (prop.isNumeric()) {
					double[] properties = new double[rows];
					for (int i = 0; i < rows; i++) {
						Object row = accessor.getRowObject(dataset.getName(), i);
						if (row instanceof Well) {
							Well well = (Well) row;
							properties[i] = prop.getValue(well);
						}
					}
					return toVector(properties);
				} else {
					String[] properties = new String[rows];
					for (int i = 0; i < rows; i++) {
						Object row = accessor.getRowObject(dataset.getName(), i);
						if (row instanceof Well) {
							Well well = (Well) row;
							properties[i] = prop.getStringValue(well);
						}
					}
					return toVector(properties);
				}
			} else if (silo.getType() == GroupType.SUBWELL.getType()) {
				if (prop.isNumeric()) {
					double[] properties = new double[rows];
					for (int i = 0; i < rows; i++) {
						Object row = accessor.getRowObject(dataset.getName(), i);
						if (row instanceof SubWellItem) {
							SubWellItem swItem = (SubWellItem) row;
							properties[i] = prop.getValue(swItem.getWell());
						}
					}
					return toVector(properties);
				} else {
					String[] properties = new String[rows];
					for (int i = 0; i < rows; i++) {
						Object row = accessor.getRowObject(dataset.getName(), i);
						if (row instanceof SubWellItem) {
							SubWellItem swItem = (SubWellItem) row;
							properties[i] = prop.getStringValue(swItem.getWell());
						}
					}
					return toVector(properties);
				}
			} else {
				throw new CalculationException("Silos of this type are not supported.");
			}
		} catch (SiloException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}
		return null;
	}

}