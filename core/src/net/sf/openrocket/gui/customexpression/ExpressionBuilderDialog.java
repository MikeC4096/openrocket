package net.sf.openrocket.gui.customexpression;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import net.sf.openrocket.document.Simulation;
import net.sf.openrocket.gui.util.Icons;
import net.sf.openrocket.l10n.Translator;
import net.sf.openrocket.logging.LogHelper;
import net.sf.openrocket.simulation.CustomExpression;
import net.sf.openrocket.startup.Application;

/**
 * Dialog box for making a custom expression
 * @author Richard Graham
 *
 */

public class ExpressionBuilderDialog extends JDialog {

	private static final Translator trans = Application.getTranslator();
	private static final LogHelper log = Application.getLogger();
	
	private static final ImageIcon GreenIcon = Icons.loadImageIcon("pix/spheres/green-16x16.png", "OK");
	private static final ImageIcon RedIcon = Icons.loadImageIcon("pix/spheres/red-16x16.png", "Bad");
	
	private CustomExpression expression;
	private CustomExpression previousExpressionCopy;
	
	private final Window parentWindow;
	private final Simulation simulation;
	
	// Define these check indicators to show if fields are OK
	private final JLabel nameCheck = new JLabel(RedIcon);
	private final JLabel expressionCheck = new JLabel(RedIcon);
	private final JLabel unitCheck = new JLabel(RedIcon);
	private final JButton okButton = new JButton(trans.get("dlg.but.ok"));
	private final JTextField expressionField = new JTextField(20);
	
	public ExpressionBuilderDialog(Window parent, Simulation simulation){
		this(parent, simulation, new CustomExpression(simulation));
	}
	
	public ExpressionBuilderDialog(Window parent, final Simulation simulation, final CustomExpression previousExpression){
		
		super(parent, trans.get("ExpressionBuilderDialog.title"), JDialog.ModalityType.DOCUMENT_MODAL);
		
		this.parentWindow = parent;
		this.simulation = simulation;
		this.previousExpressionCopy = (CustomExpression) previousExpression.clone();
		this.expression = previousExpression;
					
		//// Name box -- Check input when focus changes and transfer focus to next box on enter key
		JLabel nameLabel = new JLabel(trans.get("customExpression.Name"));
		final JTextField nameField = new JTextField(20); 
		nameField.setText(expression.getName());
		nameField.setFocusTraversalKeysEnabled(true);
		nameField.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) { }

			@Override
			public void focusLost(FocusEvent e) {
				expression.setName(nameField.getText());
				ExpressionBuilderDialog.this.updateOK();				
			}
		});
		nameField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				nameField.transferFocus();
			}
		});
		
		//// Expression box -- for this one we check after each keypress using a keyListener. Enter transfers to next field
		JLabel expressionLabel = new JLabel(trans.get("customExpression.Expression"));
		expressionField.setText(expression.getExpressionString());
		expressionField.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent arg0) {
				expression.setExpression(  expressionField.getText() );
				ExpressionBuilderDialog.this.updateOK();
			}

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyTyped(KeyEvent e) {}
		});
		expressionField.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				expressionField.transferFocus();
			}
		});
		
		//// Units box -- with action listeners checking input after change in focus or enter press
		JLabel unitLabel = new JLabel(trans.get("customExpression.Units"));
		final JTextField unitField = new JTextField(5);
		unitField.setText(expression.getUnit());
		unitField.addFocusListener(new FocusListener(){
			@Override
			public void focusLost(FocusEvent arg0) { 
				expression.setUnit(unitField.getText()) ;
				ExpressionBuilderDialog.this.updateOK();
			}
			@Override
			public void focusGained(FocusEvent arg0) {}			
		});
		unitField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				unitField.transferFocus();
			}
		});
		
		//// Symbol box
		JLabel symbolLabel = new JLabel(trans.get("customExpression.Symbol"));
		final JTextField symbolField = new JTextField(5);
		symbolField.setText(expression.getSymbol());
		symbolField.addFocusListener(new FocusListener(){
			@Override
			public void focusLost(FocusEvent arg0) { 
				expression.setSymbol(symbolField.getText()) ;
				ExpressionBuilderDialog.this.updateOK();
			}
			@Override
			public void focusGained(FocusEvent arg0) {}			
		});
		symbolField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				symbolField.transferFocus();
			}
		});
		
		
		//// Insert variable button
		final JButton insertVariableButton = new JButton(trans.get("ExpressionBuilderDialog.InsertVariable"));
		insertVariableButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				log.debug("Opening insert variable window");
				Window parentWindow = SwingUtilities.getWindowAncestor(ExpressionBuilderDialog.this);
				new VariableSelector(parentWindow, ExpressionBuilderDialog.this, simulation).setVisible(true);
			}
		});
		
		//// Insert operator button
		final JButton insertOperatorButton = new JButton(trans.get("ExpressionBuilderDialog.InsertOperator"));
		insertOperatorButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				log.debug("Opening insert operator window");
				Window parentWindow = SwingUtilities.getWindowAncestor(ExpressionBuilderDialog.this);
				new OperatorSelector(parentWindow, ExpressionBuilderDialog.this).setVisible(true);
			}
		});
		
		
		//// OK Button
		okButton.setEnabled(false);
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				expression.addToSimulation();
				ExpressionBuilderDialog.this.dispose();
			}
		});

		//// Cancel button
		final JButton cancelButton = new JButton(trans.get("dlg.but.cancel"));
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				expression.overwrite(previousExpressionCopy);
				ExpressionBuilderDialog.this.dispose();
			}
		});
			
		//// Set to tips
		nameCheck.setToolTipText(trans.get("ExpressionBuilderDialog.led.ttip.Name"));
		unitCheck.setToolTipText(trans.get("ExpressionBuilderDialog.led.ttip.Symbol"));
		expressionCheck.setToolTipText(trans.get("ExpressionBuilderDialog.led.ttip.Expression"));
		
		//// Do the layout
		JPanel mainPanel = new JPanel(new MigLayout());
		mainPanel.add(nameLabel);
		mainPanel.add(nameField);
		mainPanel.add(nameCheck, "wrap, center");
		mainPanel.add(symbolLabel);
		mainPanel.add(symbolField, "split 4, growx");
		mainPanel.add(new JPanel());
		mainPanel.add(unitLabel, "right");
		mainPanel.add(unitField, "right, growx");
		mainPanel.add(unitCheck, "wrap, center");
		mainPanel.add(expressionLabel);
		mainPanel.add(expressionField);
		mainPanel.add(expressionCheck, "wrap, center");
		mainPanel.add(insertOperatorButton, "span 2, right, split 2");
		mainPanel.add(insertVariableButton, "right, wrap");
		mainPanel.add(cancelButton, "span 2, right, width :50:100");
		mainPanel.add(okButton, "right, width :50:100, wrap");

		this.add(mainPanel);
		this.validate();
		this.pack();
		this.setLocationByPlatform(true);
		this.updateOK();
		
	}

	/**
	 * Enable OK button only if all the fields are ok
	 * @param okButton
	 */
	protected void updateOK() {
		
		boolean nameOK = expression.checkName();
		boolean unitOK = expression.checkUnit();
		boolean symbolOK = expression.checkSymbol();
		boolean expressionOK = expression.checkExpression();
		
		if (nameOK)				{ nameCheck.setIcon(GreenIcon); 		} else { nameCheck.setIcon(RedIcon); }
		if (unitOK && symbolOK)	{ unitCheck.setIcon(GreenIcon); 		} else { unitCheck.setIcon(RedIcon); }
		if (expressionOK)		{ expressionCheck.setIcon(GreenIcon); 	} else { expressionCheck.setIcon(RedIcon); }
		
		okButton.setEnabled( nameOK && unitOK && symbolOK && expressionOK );
	}
	
	/**
	 * Inserts a string into the expression box at the position of the cursor.
	 * String will be padded with spaces either side
	 * Expression box will be focused after this is called.
	 * For strings containing an ( , cursor will be moved to the point after that, otherwise, cursor will move to the end of the inserted string.
	 * @param str
	 */
	public void pasteIntoExpression(String str) {
	    int pos = expressionField.getCaretPosition();
	    String current = expressionField.getText();
	    expressionField.setText(current.subSequence(0, pos) + " " + str + " " + current.subSequence(pos, current.length()));
	    expressionField.requestFocus();
	    int bracketPos = str.indexOf("(");
	    if (bracketPos != -1){
	    	expressionField.setCaretPosition(pos+2+bracketPos);
	    }
	    else {
	    	expressionField.setCaretPosition(pos+2+str.length());
	    }
	}
}
