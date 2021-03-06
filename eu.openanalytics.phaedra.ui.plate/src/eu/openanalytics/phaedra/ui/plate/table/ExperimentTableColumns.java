package eu.openanalytics.phaedra.ui.plate.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnEditingFactory;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ProgressBarLabelProvider;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.ui.plate.util.ExperimentSummaryLoader;

public class ExperimentTableColumns {
	public static ColumnConfiguration[] configureColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		addGeneralColumns(configs);
		addProtocolColumns(configs);
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	public static ColumnConfiguration[] configureColumns(ExperimentSummaryLoader summaryLoader) {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		addGeneralColumns(configs);
		addSummaryColumns(configs, summaryLoader);
		addProtocolColumns(configs);
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	private static void addGeneralColumns(List<ColumnConfiguration> configs) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("", ColumnDataType.String, 30);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setImage(IconManager.getIconImage("map.png"));
				cell.setText("");
			}
		});
		configs.add(config);

		Function<Object, Boolean> canEditColumn = (e) -> SecurityService.getInstance().check(Permissions.EXPERIMENT_EDIT, e);
		Consumer<Object> saver = (e) -> PlateService.getInstance().updateExperiment((Experiment)e);
		
		config = ColumnConfigFactory.create("Experiment Id", "getId", ColumnDataType.Numeric, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Name", "getName", ColumnDataType.String, 250);
		config.setEditingConfig(ColumnEditingFactory.create("getName", "setName", saver, canEditColumn));
		configs.add(config);

		config = ColumnConfigFactory.create("Created On", "getCreateDate", ColumnDataType.Date, 75);
		configs.add(config);

		config = ColumnConfigFactory.create("Creator", "getCreator", ColumnDataType.String, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Description", "getDescription", ColumnDataType.String, 200);
		config.setEditingConfig(ColumnEditingFactory.create("getDescription", "setDescription", saver, canEditColumn));
		configs.add(config);
	}

	private static  void addSummaryColumns(List<ColumnConfiguration> configs, ExperimentSummaryLoader summaryLoader) {
		ColumnConfiguration config;
		Color progressColor = new Color(Display.getCurrent(), 170, 255, 170);

		config = ColumnConfigFactory.create("#P", ColumnDataType.Numeric, 50);
		CellLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return "" + summaryLoader.getSummary(exp).plates;
			}
		};
		config.setLabelProvider(labelProvider);
		config.setTooltip("Number of Plates");
		config.setSorter(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				return Integer.compare(summaryLoader.getSummary(o1).plates, summaryLoader.getSummary(o2).plates);
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("#PC", ColumnDataType.Numeric, 50);
		labelProvider = new ProgressBarLabelProvider(config, null, progressColor) {
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return ""+ summaryLoader.getSummary(exp).platesToCalculate;
			}
			@Override
			protected double getPercentage(Object element) {
				Experiment exp = (Experiment)element;
				int total = summaryLoader.getSummary(exp).plates;
				int todo = summaryLoader.getSummary(exp).platesToCalculate;
				double pct = (double)(total-todo)/total;
				return pct;
			}
		};
		config.setLabelProvider(labelProvider);
		config.setTooltip("Number of Plates to calculate");
		config.setSorter(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				return Integer.compare(summaryLoader.getSummary(o1).platesToCalculate, summaryLoader.getSummary(o2).platesToCalculate);
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("#PV", ColumnDataType.Numeric, 50);
		labelProvider = new ProgressBarLabelProvider(config, null, progressColor) {
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return "" + summaryLoader.getSummary(exp).platesToValidate;
			}
			@Override
			protected double getPercentage(Object element) {
				Experiment exp = (Experiment)element;
				int total = summaryLoader.getSummary(exp).plates;
				int todo = summaryLoader.getSummary(exp).platesToValidate;
				double pct = (double)(total-todo)/total;
				return pct;
			}
		};
		config.setLabelProvider(labelProvider);
		config.setTooltip("Number of Plates to validate");
		config.setSorter(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				return Integer.compare(summaryLoader.getSummary(o1).platesToValidate, summaryLoader.getSummary(o2).platesToValidate);
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("#PA", ColumnDataType.Numeric, 50);
		labelProvider = new ProgressBarLabelProvider(config, null, progressColor) {
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return "" + summaryLoader.getSummary(exp).platesToApprove;
			}
			@Override
			protected double getPercentage(Object element) {
				Experiment exp = (Experiment)element;
				int total = summaryLoader.getSummary(exp).plates;
				int todo = summaryLoader.getSummary(exp).platesToApprove;
				double pct = (double)(total-todo)/total;
				return pct;
			}
		};
		config.setLabelProvider(labelProvider);
		config.setTooltip("Number of Plates to approve");
		config.setSorter(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				return Integer.compare(summaryLoader.getSummary(o1).platesToApprove, summaryLoader.getSummary(o2).platesToApprove);
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("#PE", ColumnDataType.Numeric, 50);
		labelProvider = new ProgressBarLabelProvider(config, null, progressColor) {
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return "" + summaryLoader.getSummary(exp).platesToExport;
			}
			@Override
			protected double getPercentage(Object element) {
				Experiment exp = (Experiment)element;
				int total = summaryLoader.getSummary(exp).plates;
				int todo = summaryLoader.getSummary(exp).platesToExport;
				double pct = (double)(total-todo)/total;
				return pct;
			}
		};
		config.setLabelProvider(labelProvider);
		config.setTooltip("Number of Plates to export");
		config.setSorter(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				return Integer.compare(summaryLoader.getSummary(o1).platesToExport, summaryLoader.getSummary(o2).platesToExport);
			}
		});
		configs.add(config);

		// Compound counts -----------------------

		config = ColumnConfigFactory.create("#DRC", ColumnDataType.Numeric, 50);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return "" + summaryLoader.getSummary(exp).crcCount;
			}
		};
		config.setLabelProvider(labelProvider);
		config.setTooltip("Number of Compounds with Dose-Response Curves");
		config.setSorter(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				return Integer.compare(summaryLoader.getSummary(o1).crcCount, summaryLoader.getSummary(o2).crcCount);
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("#SDP", ColumnDataType.Numeric, 50);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return "" + summaryLoader.getSummary(exp).screenCount;
			}
		};
		config.setLabelProvider(labelProvider);
		config.setTooltip("Number of Single-Dose Points");
		config.setSorter(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				return Integer.compare(summaryLoader.getSummary(o1).screenCount, summaryLoader.getSummary(o2).screenCount);
			}
		});
		configs.add(config);
	}

	private static void addProtocolColumns(List<ColumnConfiguration> configs) {
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Protocol", ColumnDataType.String, 200);
		CellLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Experiment exp = (Experiment)element;
				return exp.getProtocol().getName();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Experiment>() {
			@Override
			public int compare(Experiment o1, Experiment o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return o1.getProtocol().getName().toLowerCase().compareTo(o2.getProtocol().getName().toLowerCase());
			}
		});
		config.setTooltip("Protocol");
		configs.add(config);
		
		//TODO Archive information
	}
}
