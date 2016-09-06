package eu.openanalytics.phaedra.calculation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.script.ScriptException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.calculation.hook.CalculationHookManager;
import eu.openanalytics.phaedra.calculation.jep.JEPCalculation;
import eu.openanalytics.phaedra.calculation.jep.JEPFormulaDialog;
import eu.openanalytics.phaedra.calculation.norm.NormalizationException;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.FeatureValue;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.validation.ValidationService.PlateCalcStatus;

/**
 * This service performs plate calculations. A plate calculation consists of several steps:
 * <ol>
 * <li>Remove previous values from caches (stat cache, normalization cache, etc)</li>
 * <li>Calculate raw values for features that have a calculation formula set.</li>
 * <li>Calculate normalized values for features that have a normalization method set.</li>
 * <li>Calculate some commonly used statistics (min, mean, max, zprime, etc)</li>
 * <li>Set plate calculation status flags and trigger post-calculation events</li>
 * </ol>
 */
public class CalculationService {

	private static CalculationService instance = new CalculationService();

	private CalculationHookManager calculationHookManager;
	private Map<Plate, PlateDataAccessor> dataAccessors;

	private CalculationService() {
		// Hidden constructor
		calculationHookManager = new CalculationHookManager();
		dataAccessors = new ConcurrentHashMap<>();
	}

	public static CalculationService getInstance() {
		return instance;
	}

	/*
	 * **********
	 * Public API
	 * **********
	 */

	public PlateDataAccessor getAccessor(Plate plate) {
		PlateDataAccessor accessor = dataAccessors.get(plate);
		if (accessor == null) {
			accessor = new PlateDataAccessor(plate);
			dataAccessors.put(plate, accessor);
		}
		return accessor;
	}

	public void triggerSubWellCalculation(Plate plate) {
		calculate(plate, CalculationMode.FULL);
	}

	public void calculate(Plate plate) {
		calculate(plate, CalculationMode.NORMAL);
	}

	public void calculate(Plate plate, CalculationMode mode) {

		// Security check
		SecurityService.getInstance().checkWithException(Permissions.PLATE_CALCULATE, plate);

		if (mode == CalculationMode.LIGHT) {
			// A minor calculate only clears the stat cache and fires an event.
			// It is used when sample wells are rejected/accepted.

			// Purge all current values from the caches.
			NormalizationService.getInstance().purgeCache(plate);
			StatService.getInstance().purgeCache(plate);

			// Fire calculation event.
			ModelEvent event = new ModelEvent(plate, ModelEventType.Calculated, 0);
			ModelEventService.getInstance().fireEvent(event);
		} else {
			// NORMAL and FULL calculation modify the plate, so perform pre-calculation checks (e.g. validation) first.
			calculationHookManager.preCalculation(plate);
		}

		if (mode == CalculationMode.FULL) {
			// Recalculate calculated features with "subwell data changed" trigger
			List<Feature> features = PlateUtils.getFeatures(plate);
			Collections.sort(features, ProtocolUtils.FEATURE_CALC_SEQUENCE);
			for (Feature f: features) {
				if (!f.isNumeric()) continue;
				if (CalculationTrigger.SubwellDataChange.matches(f)) {
					getAccessor(plate).forceFeatureCalculation(f);
				}
			}
		}

		if (mode == CalculationMode.NORMAL || mode == CalculationMode.FULL) {
			EclipseLog.info("Calculating " + plate, Activator.getDefault());
			
			plate.setCalculationError("");
			plate.setCalculationStatus(PlateCalcStatus.CALCULATION_NEEDED.getCode());
			plate.setCalculationDate(new Date());

			boolean calculationOk = true;

			// Purge all current values from the caches.
			NormalizationService.getInstance().purgeCache(plate);
			StatService.getInstance().purgeCache(plate);
			StatService.getInstance().togglePersistentStatsEnabled(plate, false);

			// Reset the accessor (only the calculated features).
			getAccessor(plate).reset(false);

			// Update the feature values in the database.
			List<Feature> features = PlateUtils.getFeatures(plate);
			Collections.sort(features, ProtocolUtils.FEATURE_CALC_SEQUENCE);

			for (Feature f: features) {
				if (!f.isNumeric()) continue;

				// A calculated feature: force recalculation.
				if (CalculationTrigger.PlateRecalc.matches(f)) {
					getAccessor(plate).forceFeatureCalculation(f);
				}

				String norm = f.getNormalization();
				if (norm != null && !norm.equals(NormalizationService.NORMALIZATION_NONE)) {
					// Apply normalization and save the new values.
					try {
						double[] normValues = new double[plate.getWells().size()];
						for (Well well: plate.getWells()) {
							int row = well.getRow();
							int col = well.getColumn();
							int wellNr = PlateUtils.getWellNr(well);
							double val = NormalizationService.getInstance().getNormalizedValue(plate, f, norm, row, col);
							normValues[wellNr-1] = val;
						}
						PlateService.getInstance().updateWellDataNorm(plate, f, normValues);
					} catch (NormalizationException e) {
						if (f.isRequired()) {
							plate.setCalculationError(e.getMessage());
							calculationOk = false;
							break;
						}
					}
				}
			}

			// Calculate and save persistent stats (i.e. "quick-access" stats)
			StatService.getInstance().updatePersistentPlateStats(plate);
			StatService.getInstance().togglePersistentStatsEnabled(plate, true);

			if (calculationOk) {
				plate.setCalculationStatus(PlateCalcStatus.CALCULATION_OK.getCode());
			} else {
				plate.setCalculationStatus(PlateCalcStatus.CALCULATION_ERROR.getCode());
			}
			PlateService.getInstance().updatePlate(plate);

			// Allow post-calculation operations to be executed (e.g. curve fitting)
			calculationHookManager.postCalculation(plate);

			ModelEvent event = new ModelEvent(plate, ModelEventType.Calculated, 0);
			ModelEventService.getInstance().fireEvent(event);
		}
	}

	/* Multiplo plates and compounds
	 * *****************************
	 */
	
	public List<Plate> getMultiploPlates(Plate plate) {
		List<Plate> multiploPlates = new ArrayList<>();
		if (plate == null) return multiploPlates;

		multiploPlates.add(plate);
		if (!isMultiplo(plate.getExperiment())) return multiploPlates;

		String motherId = getMotherID(plate);
		if (motherId == null || motherId.isEmpty()) return multiploPlates;

		//TODO Optimize. This performs an excessive amount of queries in multiplo experiments.
		List<Plate> plates = PlateService.getInstance().getPlates(plate.getExperiment());
		for (Plate p: plates) {
			if (p != plate && motherId.equals(getMotherID(p))) multiploPlates.add(p);
		}

		return multiploPlates;
	}
	
	public List<Compound> getMultiploCompounds(Compound compound) {
		return getMultiploPlates(compound.getPlate()).stream()
				.flatMap(p -> PlateService.streamableList(p.getCompounds()).stream())
				.filter(c -> c.getType().equals(compound.getType()) && c.getNumber().equals(compound.getNumber()))
				.collect(Collectors.toList());
	}
	
	public boolean isMultiplo(Experiment exp) {
		return exp.isMultiplo();
	}
	
	private String getMotherID(Plate plate) {
		return PlateService.getInstance().getPlateProperty(plate, "plate-mother-id");
	}
	
	/* package */ List<FeatureValue> runCalculatedFeature(Feature f, Plate p) {
		// Note: should only be called from PlateDataAccessor so values can be cached.

		if (f == null || !f.isCalculated()) {
			throw new CalculationException("Feature " + f + " is not a calculated feature");
		}

		String formula = f.getCalculationFormula();
		if (formula == null) {
			throw new CalculationException("Cannot calculate: feature " + f + " has no formula");
		}

		List<FeatureValue> results = Collections.synchronizedList(new ArrayList<FeatureValue>());
		if (!PlateUtils.getProtocolClass(p).equals(f.getProtocolClass())) {
			// This feature belongs to another protocol class. Don't even try to calculate it.
			return results;
		}

		// Prevents JPA lazy & concurrency issues.
		List<Well> wells = new ArrayList<Well>(p.getWells());
		wells.stream().forEach(well -> well.getAdapter(ProtocolClass.class));
		PlateDataAccessor accessor = getAccessor(p);
		CalculationLanguage lang = CalculationLanguage.get(f.getCalculationLanguage());

		wells.parallelStream().forEach(well -> {
			Double numericResult = null;
			try {
				String result = lang.eval(formula, accessor, well, f);
				numericResult = Double.parseDouble(result);
			} catch (ScriptException | CalculationException | NumberFormatException e) {
				numericResult = Double.NaN;
			}

			FeatureValue calculatedValue = new FeatureValue();
			calculatedValue.setFeature(f);
			calculatedValue.setWell(well);
			calculatedValue.setRawNumericValue(numericResult);
			results.add(calculatedValue);
		});

		double[] calcValues = new double[results.size()];
		for (FeatureValue r: results) {
			calcValues[PlateUtils.getWellNr(r.getWell())-1] = r.getRawNumericValue();
		}
		// Save the newly calculated values to the database.
		PlateService.getInstance().updateWellDataRaw(p, f, calcValues);

		return results;
	}

	public static class CalculationLanguage {

		private static CalculationLanguage[] languages;
		static {
			String[] ids = ScriptService.getInstance().getEngineIds();
			languages = new CalculationLanguage[ids.length];
			for (int i = 0; i < ids.length; i++) {
				languages[i] = new CalculationLanguage(ids[i], ScriptService.getInstance().getEngineLabel(ids[i]));
			}
		}
		
		private String id;
		private String label;

		CalculationLanguage(String id, String label) {
			this.id = id;
			this.label = label;
		}

		public String getId() {
			return id;
		}
		
		public String getLabel() {
			return label;
		}
		
		public String eval(String formula, PlateDataAccessor accessor, Well well, Feature feature) throws ScriptException {
			String result = "";
			Map<String, Object> context = new HashMap<String, Object>();
			if ("jep".equals(id)) {
				result = "" + JEPCalculation.evaluate(formula, well);
			} else {
				context.put("data", accessor);
				context.put("well", well);
				context.put("feature", feature);
				Object retVal = ScriptService.getInstance().executeScript(formula, context, id);
				if (retVal != null) result = retVal.toString();
			}
			return result;
		}

		public String openEditor(Shell shell, String formula, ProtocolClass pClass) {
			String retVal = null;
			if ("jep".equals(id)) {
				JEPFormulaDialog editor = new JEPFormulaDialog(shell, pClass);
				editor.setFormula(formula);
				if (editor.open() == Window.OK) retVal = editor.getFormula();
			} else {
				StringBuilder newScript = new StringBuilder(formula);
				Dialog editor = ScriptService.getInstance().createScriptEditor(newScript, id, shell);
				if (editor != null && editor.open() == Window.OK) {
					retVal = newScript.toString();
				}
			}
			return retVal;
		}

		public static CalculationLanguage[] getLanguages() {
			return languages;
		}
		
		public static CalculationLanguage get(String id) {
			return Arrays.stream(languages).filter(l -> l.getId().equals(id)).findAny().orElse(null);
		}
	}

	public static enum CalculationTrigger {
		PlateRecalc("On every plate recalculation"),
		SubwellDataChange("Only when sub-well data changes");

		private String label;

		CalculationTrigger(String label){
			this.label = label;
		}

		public String getLabel() {
			return label;
		}

		public boolean matches(Feature f) {
			return f.isCalculated() && name().equals(f.getCalculationTrigger());
		}
	}

	public static enum CalculationMode {
		LIGHT("Light","Only reset cached values"),
		NORMAL("Normal","Calculate well features, apply normalization and fit dose-response curves"),
		FULL("Full","As Normal, but also calculate subwell-based well features");

		private String label;
		private String description;

		private CalculationMode(String label, String description) {
			this.label = label;
			this.description = description;
		}

		public String getLabel() {
			return label;
		}

		public String getDescription() {
			return description;
		}
	}
}
