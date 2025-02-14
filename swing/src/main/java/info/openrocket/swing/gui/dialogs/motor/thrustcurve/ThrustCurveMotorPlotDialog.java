package info.openrocket.swing.gui.dialogs.motor.thrustcurve;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import info.openrocket.core.l10n.Translator;
import info.openrocket.core.motor.ThrustCurveMotor;
import info.openrocket.core.startup.Application;
import info.openrocket.core.unit.UnitGroup;

import net.miginfocom.swing.MigLayout;
import info.openrocket.swing.gui.util.GUIUtil;
import info.openrocket.swing.gui.widgets.SelectColorButton;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ThrustCurveMotorPlotDialog extends JDialog {
	private static final Translator trans = Application.getTranslator();
	
	public ThrustCurveMotorPlotDialog(List<ThrustCurveMotor> motors, int selected, Window parent) {
		super(parent, "Motor thrust curves", ModalityType.APPLICATION_MODAL);
		
		JPanel panel = new JPanel(new MigLayout("fill"));
		
		// Thrust curve plot
		JFreeChart chart = ChartFactory.createXYLineChart(
				"Motor thrust curves", // title
				"Time / " + UnitGroup.UNITS_SHORT_TIME.getDefaultUnit().getUnit(), // xAxisLabel
				"Thrust / " + UnitGroup.UNITS_FORCE.getDefaultUnit().getUnit(), // yAxisLabel
				null, // dataset
				PlotOrientation.VERTICAL,
				true, // legend
				true, // tooltips
				false // urls
				);
		

		// Add the data and formatting to the plot
		XYPlot plot = chart.getXYPlot();
		
		chart.setBackgroundPaint(panel.getBackground());
		plot.setBackgroundPaint(Color.WHITE);
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
		
		ChartPanel chartPanel = new ChartPanel(chart,
				false, // properties
				true, // save
				false, // print
				true, // zoom
				true); // tooltips
		chartPanel.setMouseWheelEnabled(true);
		chartPanel.setEnforceFileExtensions(true);
		chartPanel.setInitialDelay(500);
		
		StandardXYItemRenderer renderer = new StandardXYItemRenderer();
		renderer.setBaseShapesVisible(true);
		renderer.setBaseShapesFilled(true);
		plot.setRenderer(renderer);
		

		// Create the plot data set
		XYSeriesCollection dataset = new XYSeriesCollection();

		// Add data series for selected curve first, so it will
		// render "on top" of the other curves
		// Selected thrust curve
		int n = 0;
		if (selected >= 0) {
			dataset.addSeries(generateSeries(motors.get(selected),0));
			renderer.setSeriesStroke(n, new BasicStroke(1.5f));
			renderer.setSeriesPaint(n, ThrustCurveMotorSelectionPanel.getColor(selected));
		}
		n++;
		
		// Other thrust curves
		for (int i = 0; i < motors.size(); i++) {
			if (i == selected)
				continue;
			
			ThrustCurveMotor m = motors.get(i);
			dataset.addSeries(generateSeries(m, n));
			renderer.setSeriesStroke(n, new BasicStroke(1.5f));
			renderer.setSeriesPaint(n, ThrustCurveMotorSelectionPanel.getColor(i));
			renderer.setSeriesShape(n, new Rectangle());
			n++;
		}
		
		plot.setDataset(dataset);
		
		panel.add(chartPanel, "width 600:600:, height 400:400:, grow, wrap para");
		

		// Close button
		JButton close = new SelectColorButton(trans.get("dlg.but.close"));
		close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ThrustCurveMotorPlotDialog.this.setVisible(false);
			}
		});
		panel.add(close, "right, tag close");
		

		this.add(panel);
		
		this.pack();
		GUIUtil.setDisposableDialogOptions(this, null);
	}
	
	
	private XYSeries generateSeries(ThrustCurveMotor motor, int i) {
		String label = motor.getManufacturer() + " " + motor.getDesignation();
		if ( i> 0 ) {
			label += " ("+i+")";
		}
		XYSeries series = new XYSeries(label);
		double[] time = motor.getTimePoints();
		double[] thrust = motor.getThrustPoints();
		
		for (int j = 0; j < time.length; j++) {
			series.add(time[j], thrust[j]);
		}
		return series;
	}
}
